package de.photoffice.tenant;

import static de.photoffice.identity.TestTokens.platformAdmin;
import static de.photoffice.identity.TestTokens.studioAdmin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import de.photoffice.TestcontainersConfiguration;
import java.util.UUID;
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
@Import(TestcontainersConfiguration.class)
@RecordApplicationEvents
class PlatformTenantControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ApplicationEvents events;

	@Test
	void registersStudioAndListsIt() throws Exception {
		String slug = uniqueSlug();

		create(slug, "Fotostudio Müller").andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNotEmpty())
			.andExpect(jsonPath("$.slug").value(slug))
			.andExpect(jsonPath("$.name").value("Fotostudio Müller"))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.onboardingStatus").value("PENDING"));

		mockMvc.perform(get("/api/platform/tenants").with(platformAdmin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.slug == '%s')]", slug).exists());
	}

	@Test
	void publishesRegistrationWithTheFirstStudioAdmin() throws Exception {
		String slug = uniqueSlug();

		mockMvc.perform(post("/api/platform/tenants").with(platformAdmin()).contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"slug": "%s", "name": "Studio", "adminEmail": "Maria@Example.TEST",
					 "adminFirstName": "Maria", "adminLastName": "Müller"}""".formatted(slug)))
			.andExpect(status().isCreated());

		assertThat(events.stream(TenantRegistered.class)).singleElement().satisfies(event -> {
			assertThat(event.slug()).isEqualTo(slug);
			assertThat(event.admin()).isEqualTo(new InitialStudioAdmin("maria@example.test", "Maria", "Müller"));
		});
	}

	@Test
	void requiresAdminEmail() throws Exception {
		mockMvc.perform(post("/api/platform/tenants").with(platformAdmin()).contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"slug": "%s", "name": "Studio"}""".formatted(uniqueSlug())))
			.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/platform/tenants").with(platformAdmin()).contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"slug": "%s", "name": "Studio", "adminEmail": "no-mail-address"}""".formatted(uniqueSlug())))
			.andExpect(status().isBadRequest());
	}

	@Test
	void suspendsAndReactivatesStudio() throws Exception {
		String id = JsonPath.read(create(uniqueSlug(), "Studio").andReturn().getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post("/api/platform/tenants/{id}/suspend", id).with(platformAdmin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("SUSPENDED"));
		// Repeating is harmless and publishes no second event
		mockMvc.perform(post("/api/platform/tenants/{id}/suspend", id).with(platformAdmin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("SUSPENDED"));
		mockMvc.perform(post("/api/platform/tenants/{id}/reactivate", id).with(platformAdmin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ACTIVE"));

		assertThat(events.stream(TenantStatusChanged.class).map(TenantStatusChanged::status))
			.containsExactly(TenantStatus.SUSPENDED, TenantStatus.ACTIVE);
	}

	@Test
	void suspendingUnknownStudioIsNotFound() throws Exception {
		mockMvc.perform(post("/api/platform/tenants/{id}/suspend", UUID.randomUUID()).with(platformAdmin()))
			.andExpect(status().isNotFound());
	}

	@Test
	void onlyThePlatformOperatorCanSuspendStudios() throws Exception {
		String slug = uniqueSlug();
		String id = JsonPath.read(create(slug, "Studio").andReturn().getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post("/api/platform/tenants/{id}/suspend", id).with(studioAdmin(slug)))
			.andExpect(status().isForbidden());
	}

	@Test
	void rejectsDuplicateSlug() throws Exception {
		String slug = uniqueSlug();
		create(slug, "First").andExpect(status().isCreated());

		create(slug, "Second").andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
	}

	@Test
	void rejectsInvalidSlug() throws Exception {
		create("Not A Slug!", "Studio").andExpect(status().isBadRequest());
	}

	@Test
	void requiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/platform/tenants")).andExpect(status().isUnauthorized());
	}

	private ResultActions create(String slug, String name) throws Exception {
		return mockMvc.perform(post("/api/platform/tenants").with(platformAdmin()).contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"slug": "%s", "name": "%s", "adminEmail": "admin@example.test"}""".formatted(slug, name)));
	}

	private static String uniqueSlug() {
		return "studio-" + UUID.randomUUID().toString().substring(0, 8);
	}

}
