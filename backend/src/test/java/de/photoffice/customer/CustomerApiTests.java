package de.photoffice.customer;

import static de.photoffice.identity.TestTokens.photographer;
import static de.photoffice.identity.TestTokens.studioAdmin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import de.photoffice.TestcontainersConfiguration;
import de.photoffice.tenant.InitialStudioAdmin;
import de.photoffice.tenant.TenantManagement;
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

@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
@Import(TestcontainersConfiguration.class)
class CustomerApiTests {

	private static final InitialStudioAdmin STUDIO_ADMIN = new InitialStudioAdmin("admin@example.test", null, null);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TenantManagement tenantManagement;

	@Autowired
	private ApplicationEvents events;

	private String studio;

	@BeforeEach
	void registerStudio() {
		studio = tenantManagement.register("studio-" + UUID.randomUUID().toString().substring(0, 8), "Studio", STUDIO_ADMIN)
			.slug();
	}

	@Test
	void createsReadsUpdatesAndDeletesACustomer() throws Exception {
		String location = create("""
				{"firstName": " Julia ", "lastName": "Becker", "email": "Julia.Becker@Example.test",
				 "phone": "0171 1234567", "city": "Berlin", "street": "  "}""")
			.andExpect(status().isCreated())
			.andExpect(header().exists("Location"))
			.andExpect(jsonPath("$.firstName").value("Julia"))
			.andExpect(jsonPath("$.email").value("julia.becker@example.test"))
			.andExpect(jsonPath("$.street").doesNotExist())
			.andReturn()
			.getResponse()
			.getHeader("Location");

		mockMvc.perform(get(location).with(studioAdmin(studio)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.lastName").value("Becker"))
			.andExpect(jsonPath("$.city").value("Berlin"));

		mockMvc.perform(put(location).with(studioAdmin(studio)).contentType(MediaType.APPLICATION_JSON).content("""
				{"firstName": "Julia", "lastName": "Becker-Wolf", "email": "julia.becker@example.test", "city": "Potsdam"}"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.lastName").value("Becker-Wolf"))
			.andExpect(jsonPath("$.phone").doesNotExist());

		mockMvc.perform(delete(location).with(studioAdmin(studio))).andExpect(status().isNoContent());
		mockMvc.perform(get(location).with(studioAdmin(studio))).andExpect(status().isNotFound());
		assertThat(events.stream(CustomerDeleted.class)).hasSize(1);
	}

	@Test
	void photographersMayManageCustomersToo() throws Exception {
		mockMvc.perform(post("/api/studio/customers").with(photographer(studio))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"firstName": "Paul", "lastName": "Klein", "email": "paul@example.test"}"""))
			.andExpect(status().isCreated());
	}

	@Test
	void listsSortedByNameWithSearchAndPaging() throws Exception {
		createCustomer("Anna", "Zander", "anna@example.test", "Berlin");
		createCustomer("Bernd", "Albers", "bernd@example.test", "Hamburg");
		createCustomer("Carla", "Meyer", "carla@example.test", "Berlin");

		mockMvc.perform(get("/api/studio/customers").with(studioAdmin(studio)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(3))
			.andExpect(jsonPath("$.items[0].lastName").value("Albers"))
			.andExpect(jsonPath("$.items[2].lastName").value("Zander"));

		mockMvc.perform(get("/api/studio/customers?search=BERLIN").with(studioAdmin(studio)))
			.andExpect(jsonPath("$.totalElements").value(2));

		mockMvc.perform(get("/api/studio/customers?page=1&size=2").with(studioAdmin(studio)))
			.andExpect(jsonPath("$.page").value(1))
			.andExpect(jsonPath("$.size").value(2))
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.items[0].lastName").value("Zander"));
	}

	@Test
	void searchTreatsWildcardsLiterally() throws Exception {
		createCustomer("Anna", "Zander", "anna@example.test", "Berlin");

		mockMvc.perform(get("/api/studio/customers?search=%25").with(studioAdmin(studio)))
			.andExpect(jsonPath("$.totalElements").value(0));
		mockMvc.perform(get("/api/studio/customers?search=_").with(studioAdmin(studio)))
			.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void emailIsUniquePerStudioIgnoringCase() throws Exception {
		createCustomer("Anna", "Zander", "anna@example.test", null);

		create("""
				{"firstName": "Andere", "lastName": "Anna", "email": "ANNA@example.test"}""")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Ein Kunde mit der E-Mail-Adresse anna@example.test existiert bereits."));
	}

	@Test
	void updateKeepingTheOwnEmailIsAllowedButNotTakingAnother() throws Exception {
		String anna = createCustomer("Anna", "Zander", "anna@example.test", null);
		createCustomer("Bernd", "Albers", "bernd@example.test", null);

		mockMvc.perform(put("/api/studio/customers/" + anna).with(studioAdmin(studio))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"firstName": "Anna", "lastName": "Zander", "email": "anna@example.test", "notes": "VIP"}"""))
			.andExpect(status().isOk());
		mockMvc.perform(put("/api/studio/customers/" + anna).with(studioAdmin(studio))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"firstName": "Anna", "lastName": "Zander", "email": "bernd@example.test"}"""))
			.andExpect(status().isConflict());
	}

	@Test
	void rejectsInvalidInput() throws Exception {
		create("""
				{"firstName": "Anna", "lastName": "Zander"}""").andExpect(status().isBadRequest());
		create("""
				{"firstName": "Anna", "lastName": "Zander", "email": "no-email"}""").andExpect(status().isBadRequest());
		create("""
				{"firstName": "   ", "lastName": "Zander", "email": "a@example.test"}""").andExpect(status().isBadRequest());
		create("""
				{"firstName": "Anna", "lastName": "Zander", "email": "a@example.test", "phone": "abc"}""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Telefon: nur Ziffern, Leerzeichen und + - / ( ), mindestens 5 Zeichen"));
		mockMvc.perform(get("/api/studio/customers?size=1000").with(studioAdmin(studio)))
			.andExpect(status().isBadRequest());
	}

	@Test
	void unknownCustomerIsNotFound() throws Exception {
		mockMvc.perform(get("/api/studio/customers/" + UUID.randomUUID()).with(studioAdmin(studio)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Der Kunde wurde nicht gefunden."));
	}

	@Test
	void requiresLogin() throws Exception {
		mockMvc.perform(get("/api/studio/customers")).andExpect(status().isUnauthorized());
	}

	private ResultActions create(String json) throws Exception {
		return mockMvc.perform(post("/api/studio/customers").with(studioAdmin(studio))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	private String createCustomer(String firstName, String lastName, String email, String city) throws Exception {
		String body = create("""
				{"firstName": "%s", "lastName": "%s", "email": "%s", "city": %s}"""
			.formatted(firstName, lastName, email, city == null ? "null" : "\"" + city + "\""))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(body, "$.id");
	}

}
