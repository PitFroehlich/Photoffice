package de.photoffice.gallery;

import static de.photoffice.identity.TestTokens.photographer;
import static de.photoffice.identity.TestTokens.studioAdmin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import de.photoffice.TestcontainersConfiguration;
import de.photoffice.tenant.InitialStudioAdmin;
import de.photoffice.tenant.TenantManagement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
@Import(TestcontainersConfiguration.class)
class GalleryApiTests {

	private static final InitialStudioAdmin STUDIO_ADMIN = new InitialStudioAdmin("admin@example.test", null, null);

	private static final String BASE = "/api/studio/galleries";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TenantManagement tenantManagement;

	@Autowired
	private ApplicationEvents events;

	@Autowired
	private org.springframework.jdbc.core.JdbcTemplate jdbc;

	private String studio;

	private String julia;

	private String thomas;

	@BeforeEach
	void registerStudioWithCustomers() throws Exception {
		studio = tenantManagement
			.register("studio-" + UUID.randomUUID().toString().substring(0, 8), "Studio", STUDIO_ADMIN)
			.slug();
		julia = createCustomer("Julia", "Becker");
		thomas = createCustomer("Thomas", "Neumann");
	}

	@Test
	void createsReadsUpdatesAndDeletesAGallery() throws Exception {
		String location = send(post(BASE), """
				{"name": " Hochzeit Becker ", "description": "Viel Freude!", "customerIds": ["%s"]}"""
			.formatted(julia))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("Hochzeit Becker"))
			.andExpect(jsonPath("$.status").value("DRAFT"))
			.andExpect(jsonPath("$.expired").value(false))
			.andExpect(jsonPath("$.customers[0].lastName").value("Becker"))
			.andReturn()
			.getResponse()
			.getHeader("Location");

		String inTwoWeeks = today().plusDays(14).toString();
		send(put(location), """
				{"name": "Hochzeit Becker", "expiresOn": "%s", "customerIds": ["%s", "%s"]}"""
			.formatted(inTwoWeeks, julia, thomas))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.expiresOn").value(inTwoWeeks))
			.andExpect(jsonPath("$.description").doesNotExist())
			.andExpect(jsonPath("$.customers.length()").value(2))
			.andExpect(jsonPath("$.customers[0].lastName").value("Becker"))
			.andExpect(jsonPath("$.customers[1].lastName").value("Neumann"));

		mockMvc.perform(delete(location).with(studioAdmin(studio))).andExpect(status().isNoContent());
		mockMvc.perform(get(location).with(studioAdmin(studio)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Die Galerie wurde nicht gefunden."));
		assertThat(events.stream(GalleryDeleted.class)).hasSize(1);
	}

	@Test
	void photographersManageGalleriesToo() throws Exception {
		mockMvc.perform(post(BASE).with(photographer(studio)).contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "Bewerbungsfotos", "customerIds": []}"""))
			.andExpect(status().isCreated());
	}

	@Test
	void validatesInput() throws Exception {
		send(post(BASE), """
				{"name": "   ", "customerIds": []}""").andExpect(status().isBadRequest());
		send(post(BASE), """
				{"name": "!!!", "customerIds": []}""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("Name der Galerie:")));
		send(post(BASE), """
				{"name": "Alt", "expiresOn": "%s", "customerIds": []}""".formatted(today().minusDays(1)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Das Ablaufdatum darf nicht in der Vergangenheit liegen."));
		send(post(BASE), """
				{"name": "Fremd", "customerIds": ["%s"]}""".formatted(UUID.randomUUID()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Ein ausgewählter Kunde existiert nicht."));
		send(post(BASE), """
				{"name": "Ohne Liste"}""").andExpect(status().isBadRequest());
	}

	@Test
	void publishingNeedsActivePrices() throws Exception {
		String gallery = createGallery("Hochzeit Becker", julia);

		send(post(BASE + "/" + gallery + "/publish"), "")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("Ohne aktive Preise")));

		createPrice();
		send(post(BASE + "/" + gallery + "/publish"), "")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ONLINE"))
			.andExpect(jsonPath("$.publishedAt").isNotEmpty());
		assertThat(events.stream(GalleryPublished.class))
			.singleElement()
			.satisfies(published -> assertThat(published.customerIds()).containsExactly(UUID.fromString(julia)));

		send(post(BASE + "/" + gallery + "/publish"), "")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Die Galerie ist bereits online."));
	}

	@Test
	void expiredGalleriesCannotBePublished() throws Exception {
		createPrice();
		String gallery = createGallery("Babybauch", julia);
		// Simulate that the expiry date has passed (the API rejects past dates)
		var tenant = tenantManagement.findBySlug(studio).orElseThrow().id();
		de.photoffice.tenant.TenantContext.runAs(tenant, () -> jdbc
			.update("UPDATE gallery SET expires_on = ? WHERE id = ?::uuid", today().minusDays(1), gallery));

		mockMvc.perform(get(BASE + "/" + gallery).with(studioAdmin(studio)))
			.andExpect(jsonPath("$.expired").value(true));
		send(post(BASE + "/" + gallery + "/publish"), "")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("Die Galerie ist abgelaufen.")));
		// Saving with the unchanged (past) date is allowed – only new past dates are rejected
		send(put(BASE + "/" + gallery), """
				{"name": "Babybauch", "expiresOn": "%s", "customerIds": []}""".formatted(today().minusDays(1)))
			.andExpect(status().isOk());
	}

	@Test
	void unpublishAndPublishAgain() throws Exception {
		createPrice();
		String gallery = createGallery("Hochzeit Becker", julia);

		send(post(BASE + "/" + gallery + "/unpublish"), "")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Die Galerie ist nicht online."));
		send(post(BASE + "/" + gallery + "/publish"), "").andExpect(status().isOk());
		send(post(BASE + "/" + gallery + "/unpublish"), "")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("OFFLINE"));
		send(post(BASE + "/" + gallery + "/publish"), "")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ONLINE"));
	}

	@Test
	void listsNewestFirstWithFilters() throws Exception {
		createPrice();
		String first = createGallery("Hochzeit Becker", julia);
		createGallery("Familienshooting Neumann", thomas);
		send(post(BASE + "/" + first + "/publish"), "").andExpect(status().isOk());

		mockMvc.perform(get(BASE).with(studioAdmin(studio)))
			.andExpect(jsonPath("$.totalElements").value(2))
			.andExpect(jsonPath("$.items[0].name").value("Familienshooting Neumann"));
		mockMvc.perform(get(BASE + "?search=HOCHZEIT").with(studioAdmin(studio)))
			.andExpect(jsonPath("$.totalElements").value(1));
		mockMvc.perform(get(BASE + "?status=ONLINE").with(studioAdmin(studio)))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.items[0].name").value("Hochzeit Becker"));
		mockMvc.perform(get(BASE + "?customerId=" + thomas).with(studioAdmin(studio)))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.items[0].name").value("Familienshooting Neumann"));
		mockMvc.perform(get(BASE + "?search=%25").with(studioAdmin(studio)))
			.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void deletingACustomerRemovesTheAssignmentOnly() throws Exception {
		String gallery = createGallery("Hochzeit Becker", julia);

		mockMvc.perform(delete("/api/studio/customers/" + julia).with(studioAdmin(studio)))
			.andExpect(status().isNoContent());

		mockMvc.perform(get(BASE + "/" + gallery).with(studioAdmin(studio)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.customers.length()").value(0));
	}

	private String createGallery(String name, String customerId) throws Exception {
		String body = send(post(BASE), """
				{"name": "%s", "customerIds": ["%s"]}""".formatted(name, customerId))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(body, "$.id");
	}

	private String createCustomer(String firstName, String lastName) throws Exception {
		String body = send(post("/api/studio/customers"), """
				{"firstName": "%s", "lastName": "%s", "email": "%s@example.test"}"""
			.formatted(firstName, lastName, firstName.toLowerCase()))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(body, "$.id");
	}

	private void createPrice() throws Exception {
		send(post("/api/studio/price-list/products"), """
				{"type": "DOWNLOAD", "downloadName": "Original", "priceCents": 990}""")
			.andExpect(status().isCreated());
	}

	private ResultActions send(MockHttpServletRequestBuilder request, String json) throws Exception {
		return mockMvc.perform(request.with(studioAdmin(studio)).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private static LocalDate today() {
		return LocalDate.now(ZoneId.of("Europe/Berlin"));
	}

}
