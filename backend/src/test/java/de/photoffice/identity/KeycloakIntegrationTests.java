package de.photoffice.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.photoffice.TestcontainersConfiguration;
import de.photoffice.tenant.InitialStudioAdmin;
import de.photoffice.tenant.TenantManagement;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

/**
 * End-to-end check of the Keycloak realm in {@code infra/keycloak/photoffice-realm.json} against the backend:
 * real tokens (organization claim, realm roles, audience) must lead to the right studio.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers
class KeycloakIntegrationTests {

	@Container
	static final GenericContainer<?> keycloak = new GenericContainer<>("quay.io/keycloak/keycloak:26.8")
		.withCommand("start-dev", "--import-realm")
		.withCopyFileToContainer(
				MountableFile.forHostPath(Path.of("../infra/keycloak/photoffice-realm.json").toAbsolutePath()),
				"/opt/keycloak/data/import/photoffice-realm.json")
		.withExposedPorts(8080)
		.waitingFor(Wait.forHttp("/realms/photoffice").forPort(8080).forStatusCode(200));

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

	private static String issuerUri() {
		return "http://" + keycloak.getHost() + ":" + keycloak.getMappedPort(8080) + "/realms/photoffice";
	}

	/** Dev realm users have their username as password; the dev-cli client allows the password grant. */
	private static String accessToken(String username) {
		var form = new LinkedMultiValueMap<String, String>();
		form.add("grant_type", "password");
		form.add("client_id", "photoffice-dev-cli");
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
