package de.photoffice.tenant;

import static de.photoffice.identity.TestTokens.platformAdmin;
import static de.photoffice.identity.TestTokens.studioAdmin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import de.photoffice.TestcontainersConfiguration;
import java.net.URI;
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

	@Autowired
	private TenantManagement tenantManagement;

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

		// The admin data is kept at the studio (correctable until onboarding completes), not in the event
		assertThat(events.stream(TenantRegistered.class)).singleElement().satisfies(event -> {
			assertThat(event.slug()).isEqualTo(slug);
			assertThat(event.admin()).isNull();
			assertThat(tenantManagement.findById(event.tenantId()).orElseThrow().initialAdmin())
				.hasValue(new InitialStudioAdmin("maria@example.test", "Maria", "Müller"));
		});
	}

	@Test
	void showsTheFirstStudioAdminUntilOnboardingCompletes() throws Exception {
		String id = JsonPath.read(create(uniqueSlug(), "Studio").andExpect(jsonPath("$.adminEmail").value("admin@example.test"))
			.andReturn()
			.getResponse()
			.getContentAsString(), "$.id");

		mockMvc.perform(get("/api/platform/tenants/{id}", id).with(platformAdmin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(id))
			.andExpect(jsonPath("$.adminEmail").value("admin@example.test"));

		// Data minimisation: removed once the admin exists in Keycloak
		tenantManagement.markOnboarded(TenantId.of(UUID.fromString(id)));
		mockMvc.perform(get("/api/platform/tenants/{id}", id).with(platformAdmin()))
			.andExpect(jsonPath("$.onboardingStatus").value("COMPLETED"))
			.andExpect(jsonPath("$.adminEmail").value(nullValue()));
	}

	@Test
	void gettingUnknownOrInvalidStudio() throws Exception {
		mockMvc.perform(get("/api/platform/tenants/{id}", UUID.randomUUID()).with(platformAdmin()))
			.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/platform/tenants/{id}", "abc").with(platformAdmin()))
			.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/platform/tenants/{id}", UUID.randomUUID()).with(studioAdmin("studio-a")))
			.andExpect(status().isForbidden());
	}

	@Test
	void changesNameAndFirstStudioAdminWhileOnboardingIsPending() throws Exception {
		String slug = uniqueSlug();
		String id = JsonPath.read(create(slug, "Studio").andReturn().getResponse().getContentAsString(), "$.id");

		update(id, """
				{"name": "  Fotostudio Neu ", "adminEmail": "Neu@Example.TEST", "adminFirstName": "Nina"}""")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.slug").value(slug))
			.andExpect(jsonPath("$.name").value("Fotostudio Neu"))
			.andExpect(jsonPath("$.adminEmail").value("neu@example.test"))
			.andExpect(jsonPath("$.adminFirstName").value("Nina"))
			.andExpect(jsonPath("$.adminLastName").value(nullValue()))
			.andExpect(jsonPath("$.onboardingStatus").value("PENDING"));

		assertThat(events.stream(TenantRenamed.class)).singleElement()
			.isEqualTo(new TenantRenamed(TenantId.of(UUID.fromString(id)), slug, "Fotostudio Neu"));

		// Without admin fields the admin stays as it is; an unchanged name publishes no event
		update(id, """
				{"name": "Fotostudio Neu"}""").andExpect(status().isOk())
			.andExpect(jsonPath("$.adminEmail").value("neu@example.test"))
			.andExpect(jsonPath("$.adminFirstName").value("Nina"));
		assertThat(events.stream(TenantRenamed.class)).hasSize(1);
	}

	@Test
	void firstStudioAdminCannotBeChangedAfterOnboarding() throws Exception {
		String id = JsonPath.read(create(uniqueSlug(), "Studio Fertig").andReturn().getResponse().getContentAsString(),
				"$.id");
		tenantManagement.markOnboarded(TenantId.of(UUID.fromString(id)));

		update(id, """
				{"name": "Studio Fertig", "adminEmail": "anders@example.test"}""").andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("Das Onboarding von „Studio Fertig“ ist bereits abgeschlossen – die "
					+ "Daten des ersten Studio-Admins lassen sich nicht mehr ändern."));

		// The name can still be changed
		update(id, """
				{"name": "Studio Umbenannt"}""").andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Studio Umbenannt"))
			.andExpect(jsonPath("$.onboardingStatus").value("COMPLETED"));
	}

	@Test
	void rejectsInvalidChanges() throws Exception {
		String id = JsonPath.read(create(uniqueSlug(), "Studio").andReturn().getResponse().getContentAsString(), "$.id");

		update(id, """
				{"name": "   "}""").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Bitte einen Namen für das Studio angeben."));
		update(id, """
				{"adminEmail": "admin@example.test"}""").andExpect(status().isBadRequest());
		update(id, """
				{"name": "Studio", "adminEmail": "no-mail-address"}""").andExpect(status().isBadRequest());
		update(id, """
				{"name": "Studio", "adminFirstName": "Nina"}""").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail")
				.value("Vor- und Nachname des ersten Studio-Admins nur zusammen mit der E-Mail-Adresse angeben."));
		update(UUID.randomUUID().toString(), """
				{"name": "Studio"}""").andExpect(status().isNotFound());
		mockMvc.perform(patch("/api/platform/tenants/{id}", id).with(studioAdmin("studio-a"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "Übernommen"}""")).andExpect(status().isForbidden());

		mockMvc.perform(get("/api/platform/tenants/{id}", id).with(platformAdmin()))
			.andExpect(jsonPath("$.name").value("Studio"))
			.andExpect(jsonPath("$.adminEmail").value("admin@example.test"));
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
	void invalidStudioIdIsBadRequestWithProblemDetail() throws Exception {
		mockMvc.perform(post("/api/platform/tenants/{id}/suspend", "abc").with(platformAdmin()))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("Ungültige Studio-ID „abc“ – erwartet wird eine UUID."));
	}

	@Test
	void emptyStudioIdIsBadRequestWithProblemDetail() throws Exception {
		// Rejected by Spring Security's firewall before authentication (used to be an empty 401)
		// URI instead of a template: templates collapse "//"
		mockMvc.perform(post(URI.create("/api/platform/tenants//suspend")).with(platformAdmin()))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.detail").value(containsString("Anfrage-URL ist ungültig")));
	}

	@Test
	void showsTheLastOnboardingFailureUntilOnboardingSucceeds() throws Exception {
		String slug = uniqueSlug();
		String id = JsonPath.read(create(slug, "Studio").andReturn().getResponse().getContentAsString(), "$.id");
		TenantId tenantId = TenantId.of(UUID.fromString(id));

		tenantManagement.recordOnboardingFailure(tenantId, "Keycloak ist nicht erreichbar.");
		mockMvc.perform(get("/api/platform/tenants").with(platformAdmin()))
			.andExpect(jsonPath("$[?(@.slug == '%s')].onboardingStatus", slug).value("PENDING"))
			.andExpect(jsonPath("$[?(@.slug == '%s')].onboardingError", slug).value("Keycloak ist nicht erreichbar."))
			.andExpect(jsonPath("$[?(@.slug == '%s')].onboardingFailedAt", slug).isNotEmpty());

		tenantManagement.markOnboarded(tenantId);
		mockMvc.perform(get("/api/platform/tenants").with(platformAdmin()))
			.andExpect(jsonPath("$[?(@.slug == '%s')].onboardingStatus", slug).value("COMPLETED"))
			.andExpect(jsonPath("$[?(@.slug == '%s')].onboardingError", slug).value(contains(nullValue())))
			.andExpect(jsonPath("$[?(@.slug == '%s')].onboardingFailedAt", slug).value(contains(nullValue())));

		// A late failure report (duplicate delivery) does not bring the error back
		tenantManagement.recordOnboardingFailure(tenantId, "zu spät");
		assertThat(tenantManagement.findById(tenantId).orElseThrow().onboardingError()).isEmpty();
	}

	@Test
	void retryingOnboardingAcceptsTheRequest() throws Exception {
		String id = JsonPath.read(create(uniqueSlug(), "Studio").andReturn().getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post("/api/platform/tenants/{id}/onboarding/retry", id).with(platformAdmin()))
			.andExpect(status().isAccepted())
			.andExpect(jsonPath("$.id").value(id))
			.andExpect(jsonPath("$.onboardingStatus").value("PENDING"));
		mockMvc.perform(post("/api/platform/tenants/{id}/onboarding/retry", UUID.randomUUID()).with(platformAdmin()))
			.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/platform/tenants/{id}/onboarding/retry", id).with(studioAdmin("studio-a")))
			.andExpect(status().isForbidden());
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

		create(slug, "Second").andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.detail").value("Das Kürzel „" + slug + "“ ist bereits vergeben."));
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

	private ResultActions update(String id, String json) throws Exception {
		return mockMvc.perform(patch("/api/platform/tenants/{id}", id).with(platformAdmin())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	private static String uniqueSlug() {
		return "studio-" + UUID.randomUUID().toString().substring(0, 8);
	}

}
