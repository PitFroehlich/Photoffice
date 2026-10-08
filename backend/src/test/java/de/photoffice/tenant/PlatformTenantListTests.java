package de.photoffice.tenant;

import static de.photoffice.identity.TestTokens.platformAdmin;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.photoffice.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Studio list of the platform operator (issue #45): search, filters, paging. Other tests also create studios, so
 * every test works with studios carrying its own unique marker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PlatformTenantListTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TenantManagement tenantManagement;

	private String marker;

	@BeforeEach
	void marker() {
		marker = "liste" + UUID.randomUUID().toString().substring(0, 6);
	}

	@Test
	void searchesNameSlugAndAdminEmailCaseInsensitively() throws Exception {
		register("Fotostudio Müller " + marker, marker + "-mueller", "inhaber@mueller.test");
		register("Bildwerk", marker + "-bildwerk", "info@bildwerk.test");
		register("Lichtblick", "lichtblick-" + UUID.randomUUID().toString().substring(0, 6),
				"kontakt@" + marker + ".test");

		mockMvc.perform(list().param("search", marker.toUpperCase()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(3))
			// sorted by name
			.andExpect(jsonPath("$.items[*].name").value(contains("Bildwerk", "Fotostudio Müller " + marker, "Lichtblick")));

		mockMvc.perform(list().param("search", "MÜLLER " + marker))
			.andExpect(jsonPath("$.items[*].slug").value(contains(marker + "-mueller")));
		mockMvc.perform(list().param("search", "kontakt@" + marker))
			.andExpect(jsonPath("$.items[*].name").value(contains("Lichtblick")));
	}

	@Test
	void likeWildcardsAreSearchedLiterally() throws Exception {
		register("Studio 100% " + marker, marker + "-prozent", "a@example.test");
		register("Studio 1000 " + marker, marker + "-tausend", "b@example.test");

		mockMvc.perform(list().param("search", "100% " + marker))
			.andExpect(jsonPath("$.items[*].slug").value(contains(marker + "-prozent")));
		mockMvc.perform(list().param("search", marker + "_"))
			.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void pagesThroughTheResults() throws Exception {
		for (int i = 1; i <= 5; i++) {
			register("Studio " + marker + " " + i, marker + "-" + i, "admin" + i + "@example.test");
		}

		mockMvc.perform(list().param("search", marker).param("page", "1").param("size", "2"))
			.andExpect(jsonPath("$.page").value(1))
			.andExpect(jsonPath("$.size").value(2))
			.andExpect(jsonPath("$.totalElements").value(5))
			.andExpect(jsonPath("$.items[*].slug").value(contains(marker + "-3", marker + "-4")));
		mockMvc.perform(list().param("search", marker).param("page", "2").param("size", "2"))
			.andExpect(jsonPath("$.items[*].slug").value(contains(marker + "-5")));
		mockMvc.perform(list().param("search", marker))
			.andExpect(jsonPath("$.size").value(25))
			.andExpect(jsonPath("$.items.length()").value(5));
	}

	@Test
	void filtersByStatusAndOnboarding() throws Exception {
		register("A läuft " + marker, marker + "-laeuft", "a@example.test");
		Tenant failed = register("B fehlgeschlagen " + marker, marker + "-fehler", "b@example.test");
		Tenant done = register("C fertig " + marker, marker + "-fertig", "c@example.test");
		Tenant suspended = register("D gesperrt " + marker, marker + "-gesperrt", "d@example.test");
		tenantManagement.recordOnboardingFailure(failed.id(), "Keycloak ist nicht erreichbar.");
		tenantManagement.markOnboarded(done.id());
		tenantManagement.markOnboarded(suspended.id());
		tenantManagement.suspend(suspended.id());

		mockMvc.perform(list().param("search", marker).param("onboarding", "FAILED"))
			.andExpect(jsonPath("$.items[*].slug").value(contains(marker + "-fehler")))
			.andExpect(jsonPath("$.failedOnboardings").value(greaterThanOrEqualTo(1)));
		mockMvc.perform(list().param("search", marker).param("onboarding", "PENDING"))
			.andExpect(jsonPath("$.items[*].slug").value(contains(marker + "-laeuft", marker + "-fehler")));
		mockMvc.perform(list().param("search", marker).param("onboarding", "COMPLETED"))
			.andExpect(jsonPath("$.items[*].slug").value(contains(marker + "-fertig", marker + "-gesperrt")));
		mockMvc.perform(list().param("search", marker).param("status", "SUSPENDED"))
			.andExpect(jsonPath("$.items[*].slug").value(contains(marker + "-gesperrt")));
		mockMvc.perform(list().param("search", marker).param("status", "ACTIVE").param("onboarding", "COMPLETED"))
			.andExpect(jsonPath("$.items[*].slug").value(contains(marker + "-fertig")));
	}

	@Test
	void adminEmailIsNoLongerFoundAfterOnboarding() throws Exception {
		Tenant tenant = register("Studio " + marker, marker + "-studio", "geheim@" + marker + ".test");
		mockMvc.perform(list().param("search", "geheim@" + marker)).andExpect(jsonPath("$.totalElements").value(1));

		tenantManagement.markOnboarded(tenant.id());

		// Data minimisation: the address is deleted, so it cannot be found anymore
		mockMvc.perform(list().param("search", "geheim@" + marker)).andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void rejectsInvalidParameters() throws Exception {
		mockMvc.perform(list().param("status", "GELOESCHT"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value(
					"Ungültiger Wert „GELOESCHT“ für den Parameter „status“ – erlaubt: [ACTIVE, SUSPENDED]."));
		mockMvc.perform(list().param("onboarding", "failed")).andExpect(status().isBadRequest());
		mockMvc.perform(list().param("size", "101")).andExpect(status().isBadRequest());
		mockMvc.perform(list().param("page", "-1")).andExpect(status().isBadRequest());
		mockMvc.perform(list().param("search", "x".repeat(101))).andExpect(status().isBadRequest());
	}

	private Tenant register(String name, String slug, String adminEmail) {
		return tenantManagement.register(slug, name, new InitialStudioAdmin(adminEmail, null, null));
	}

	private static MockHttpServletRequestBuilder list() {
		return get("/api/platform/tenants").with(platformAdmin());
	}

}
