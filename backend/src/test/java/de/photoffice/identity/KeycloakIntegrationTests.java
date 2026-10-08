package de.photoffice.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.photoffice.TestcontainersConfiguration;
import de.photoffice.tenant.InitialStudioAdmin;
import de.photoffice.tenant.TenantManagement;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * End-to-end check of the Keycloak realm in {@code infra/keycloak/photoffice-realm.json} against the backend:
 * real tokens (organization claim, realm roles, audience) must lead to the right studio; the login pages use the
 * German Photoffice theme (issue #36); the Keycloak account console is switched off and users may change their name
 * but not their e-mail address (issue #46, ADR 0013).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers
class KeycloakIntegrationTests {

	@Container
	static final GenericContainer<?> keycloak = DevKeycloakContainer.create();

	@DynamicPropertySource
	static void issuer(DynamicPropertyRegistry registry) {
		registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", KeycloakIntegrationTests::issuerUri);
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TenantManagement tenantManagement;

	@BeforeEach
	void registerStudiosOfTheDevRealm() {
		for (String slug : new String[] { "studio-a", "studio-b" }) {
			if (tenantManagement.findBySlug(slug).isEmpty()) {
				tenantManagement.register(slug, slug.toUpperCase(), new InitialStudioAdmin("admin@" + slug + ".test", null, null));
			}
		}
	}

	@Test
	void studioAdminOfStudioA() throws Exception {
		mockMvc.perform(get("/api/studio/me").header("Authorization", "Bearer " + accessToken("admin-a")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value("admin-a"))
			.andExpect(jsonPath("$.email").value("admin@studio-a.test"))
			.andExpect(jsonPath("$.studio.slug").value("studio-a"))
			.andExpect(jsonPath("$.roles[0]").value("STUDIO_ADMIN"));
	}

	@Test
	void photographerOfStudioA() throws Exception {
		mockMvc.perform(get("/api/studio/me").header("Authorization", "Bearer " + accessToken("foto-a")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.studio.slug").value("studio-a"))
			.andExpect(jsonPath("$.roles[0]").value("PHOTOGRAPHER"));
	}

	@Test
	void studioAdminOfStudioB() throws Exception {
		mockMvc.perform(get("/api/studio/me").header("Authorization", "Bearer " + accessToken("admin-b")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.studio.slug").value("studio-b"));
	}

	@Test
	void platformOperator() throws Exception {
		String token = accessToken("operator");

		mockMvc.perform(get("/api/platform/tenants").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk());
		mockMvc.perform(get("/api/studio/me").header("Authorization", "Bearer " + token))
			.andExpect(status().isForbidden());
	}

	@Test
	void studioAdminCannotManageStudios() throws Exception {
		mockMvc.perform(get("/api/platform/tenants").header("Authorization", "Bearer " + accessToken("admin-a")))
			.andExpect(status().isForbidden());
	}

	@Test
	void invalidTokenIsRejected() throws Exception {
		String tampered = accessToken("admin-a").replaceFirst("\\.[^.]+$", ".invalidsignature");

		mockMvc.perform(get("/api/studio/me").header("Authorization", "Bearer " + tampered))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void loginPageIsGermanAndUsesThePhotofficeTheme() {
		// German even if the browser prefers English: the realm offers German only (issue #36)
		String loginPage = RestClient.create()
			.get()
			.uri(issuerUri() + "/protocol/openid-connect/auth?client_id=photoffice-frontend&response_type=code&scope=openid"
					+ "&redirect_uri=http://localhost:4200/&code_challenge_method=S256"
					+ "&code_challenge=Rkz4ypGlYEXqCTI0h1vZ8_wWbGUDaqlAEanUuBb_r9U")
			.header(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9")
			.retrieve()
			.body(String.class);

		assertThat(loginPage).contains("lang=\"de\"")
			.contains("Bei Photoffice anmelden")
			.contains("/login/photoffice/css/photoffice.css");
	}

	@Test
	void accountConsoleIsSwitchedOff() {
		// Neither the console nor its REST API – not even with a token of a dev user (admin-cli allows the password
		// grant in every realm and its tokens would otherwise be accepted by the account REST API)
		String userToken = accessToken("admin-a", "admin-cli");
		for (String path : List.of("/account", "/account/", "/account/credentials")) {
			HttpStatusCode status = RestClient.create()
				.get()
				.uri(issuerUri() + path)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
				.accept(MediaType.APPLICATION_JSON)
				.exchange((request, response) -> response.getStatusCode());
			assertThat(status.value()).as(path).isEqualTo(404);
		}
	}

	@Test
	void defaultRolesGrantNoAccountManagement() {
		List<?> composites = adminApi().get()
			.uri("/roles/default-roles-photoffice/composites")
			.retrieve()
			.body(List.class);
		List<String> names = composites.stream().map(role -> String.valueOf(((Map<?, ?>) role).get("name"))).toList();

		assertThat(names).doesNotContain("manage-account", "view-profile").contains("offline_access");
	}

	@Test
	@SuppressWarnings("unchecked")
	void usersMayChangeTheirNameButNotTheirEmailAddress() {
		Map<String, Object> profile = adminApi().get().uri("/users/profile").retrieve().body(Map.class);
		var attributes = (List<Map<String, Object>>) profile.get("attributes");

		assertThat(editableBy(attributes, "email")).containsExactly("admin");
		assertThat(editableBy(attributes, "username")).containsExactly("admin");
		assertThat(editableBy(attributes, "firstName")).contains("user", "admin");
		assertThat(editableBy(attributes, "lastName")).contains("user", "admin");
	}

	@SuppressWarnings("unchecked")
	private static List<String> editableBy(List<Map<String, Object>> attributes, String name) {
		return attributes.stream()
			.filter(attribute -> name.equals(attribute.get("name")))
			.map(attribute -> (List<String>) ((Map<String, Object>) attribute.get("permissions")).get("edit"))
			.findFirst()
			.orElseThrow();
	}

	/** Admin REST API of the realm with the bootstrap admin (admin / admin, master realm). */
	private static RestClient adminApi() {
		var form = new LinkedMultiValueMap<String, String>();
		form.add("grant_type", "password");
		form.add("client_id", "admin-cli");
		form.add("username", "admin");
		form.add("password", "admin");
		Map<?, ?> token = RestClient.create()
			.post()
			.uri(keycloakUrl() + "/realms/master/protocol/openid-connect/token")
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(form)
			.retrieve()
			.body(Map.class);
		assertThat(token).isNotNull();
		return RestClient.builder()
			.baseUrl(keycloakUrl() + "/admin/realms/photoffice")
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token.get("access_token"))
			.build();
	}

	private static String keycloakUrl() {
		return "http://" + keycloak.getHost() + ":" + keycloak.getMappedPort(8080);
	}

	private static String issuerUri() {
		return keycloakUrl() + "/realms/photoffice";
	}

	/** Dev realm users have their username as password; the dev-cli client allows the password grant. */
	private static String accessToken(String username) {
		return accessToken(username, "photoffice-dev-cli");
	}

	private static String accessToken(String username, String clientId) {
		var form = new LinkedMultiValueMap<String, String>();
		form.add("grant_type", "password");
		form.add("client_id", clientId);
		form.add("username", username);
		form.add("password", username);
		Map<?, ?> response = RestClient.create()
			.post()
			.uri(issuerUri() + "/protocol/openid-connect/token")
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(form)
			.retrieve()
			.body(Map.class);
		assertThat(response).isNotNull();
		assertThat(response.get("access_token")).as("access token").isInstanceOf(String.class);
		return (String) response.get("access_token");
	}

}
