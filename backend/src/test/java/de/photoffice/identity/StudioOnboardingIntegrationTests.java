package de.photoffice.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.photoffice.TestcontainersConfiguration;
import de.photoffice.tenant.InitialStudioAdmin;
import de.photoffice.tenant.Tenant;
import de.photoffice.tenant.TenantManagement;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Studio onboarding against a real Keycloak with the dev realm ({@code infra/keycloak/photoffice-realm.json}) and
 * Mailpit as SMTP server: organization, first studio admin, invitation e-mail (Photoffice theme), suspension.
 */
@SpringBootTest(properties = { "photoffice.onboarding.enabled=true", "photoffice.events.resubmission.interval=PT2S",
		"photoffice.events.resubmission.min-age=PT0S" })
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers
class StudioOnboardingIntegrationTests {

	static final Network network = Network.newNetwork();

	/** Host name "mailpit" matches the SMTP settings of the dev realm. */
	@Container
	static final GenericContainer<?> mailpit = new GenericContainer<>("axllent/mailpit:v1.27").withNetwork(network)
		.withNetworkAliases("mailpit")
		.withExposedPorts(8025)
		.waitingFor(Wait.forHttp("/readyz").forPort(8025));

	@Container
	static final GenericContainer<?> keycloak = DevKeycloakContainer.create().withNetwork(network);

	@DynamicPropertySource
	static void keycloakProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> keycloakUrl() + "/realms/photoffice");
		registry.add("photoffice.keycloak.server-url", StudioOnboardingIntegrationTests::keycloakUrl);
	}

	@Autowired
	private TenantManagement tenantManagement;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private KeycloakAdminClient keycloakAdmin;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void createsOrganizationAndInvitesTheFirstStudioAdmin() throws Exception {
		String slug = uniqueSlug();
		String email = "inhaber@" + slug + ".test";

		Tenant tenant = tenantManagement.register(slug, "Fotostudio Test", new InitialStudioAdmin(email, "Ina", "Haber"));
		awaitOnboarded(tenant);

		KeycloakAdminClient.Organization organization = keycloakAdmin.findOrganization(slug).orElseThrow();
		assertThat(organization.name()).isEqualTo(slug);
		assertThat(organization.description()).isEqualTo("Fotostudio Test");
		assertThat(organization.enabled()).isTrue();

		KeycloakAdminClient.User admin = keycloakAdmin.findUserByEmail(email).orElseThrow();
		assertThat(keycloakAdmin.organizationsOf(admin.id())).containsExactly(slug);
		assertThat(admin.requiredActions()).contains(StudioOnboarding.UPDATE_PASSWORD);

		// Invitation e-mail with Keycloak's action link (no password in the mail), German Photoffice e-mail theme
		Map<String, Object> mail = awaitMailTo(email);
		assertThat(mail.get("Subject")).isEqualTo("Willkommen bei Photoffice – bitte richten Sie Ihren Zugang ein");
		assertThat(((Map<?, ?>) mail.get("From")).get("Name")).isEqualTo("Photoffice");
		assertThat((String) mail.get("Text")).contains("Hallo Ina Haber,")
			.contains("Passwort festlegen")
			.contains(keycloakUrl() + "/realms/photoffice/login-actions/action-token");
		assertThat((String) mail.get("HTML")).contains(">Passwort festlegen</a>");

		// After setting the password the admin is a member of the new studio with role studio-admin
		completeInvitation(admin.id(), "Geheim-123");
		mockMvc.perform(get("/api/studio/me").header("Authorization", "Bearer " + accessToken(email, "Geheim-123")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.studio.slug").value(slug))
			.andExpect(jsonPath("$.roles[0]").value("STUDIO_ADMIN"));
	}

	@Test
	void completesOnboardingThatFailedHalfway() {
		String slug = uniqueSlug();
		// Organization from an interrupted earlier attempt
		keycloakAdmin.createOrganization(slug, "Fotostudio Test", true);

		Tenant tenant = tenantManagement.register(slug, "Fotostudio Test",
				new InitialStudioAdmin("admin@" + slug + ".test", null, null));
		awaitOnboarded(tenant);

		String userId = keycloakAdmin.findUserByEmail("admin@" + slug + ".test").orElseThrow().id();
		assertThat(keycloakAdmin.organizationsOf(userId)).containsExactly(slug);
	}

	@Test
	void suspendingTheStudioDisablesItsOrganization() throws Exception {
		String slug = uniqueSlug();
		Tenant tenant = tenantManagement.register(slug, "Fotostudio Test",
				new InitialStudioAdmin("admin@" + slug + ".test", null, null));
		awaitOnboarded(tenant);

		tenantManagement.suspend(tenant.id());
		await().atMost(Duration.ofSeconds(30))
			.until(() -> !keycloakAdmin.findOrganization(slug).orElseThrow().enabled());

		tenantManagement.reactivate(tenant.id());
		await().atMost(Duration.ofSeconds(30)).until(() -> keycloakAdmin.findOrganization(slug).orElseThrow().enabled());
	}

	@Test
	void failedOnboardingIsRetriedUntilItSucceeds() {
		// A user of another studio cannot become admin – onboarding fails until the conflict is resolved
		String otherStudio = uniqueSlug();
		String otherStudioId = keycloakAdmin.createOrganization(otherStudio, "Anderes Studio", true);
		String email = "wechsel@" + otherStudio + ".test";
		String userId = keycloakAdmin.createUser(email, null, null, List.of()).id();
		keycloakAdmin.addMember(otherStudioId, userId);

		String slug = uniqueSlug();
		Tenant tenant = tenantManagement.register(slug, "Fotostudio Test", new InitialStudioAdmin(email, null, null));

		await().atMost(Duration.ofSeconds(30))
			.until(() -> jdbc.queryForObject(
					"SELECT coalesce(max(completion_attempts), 0) FROM event_publication WHERE serialized_event LIKE ?",
					Integer.class, "%" + slug + "%") >= 2);
		Tenant failed = tenantManagement.findById(tenant.id()).orElseThrow();
		assertThat(failed.onboarded()).isFalse();
		// The reason is recorded for the platform operator although the listener's transaction was rolled back
		assertThat(failed.onboardingError()).hasValue("Die E-Mail-Adresse " + email + " gehört bereits zu Studio „"
				+ otherStudio + "“. Ein Benutzer kann nur zu einem Studio gehören.");
		assertThat(failed.onboardingFailedAt()).isPresent();
		assertThat(keycloakAdmin.organizationsOf(userId)).containsExactly(otherStudio);

		// Conflict resolved: the next resubmission (EventResubmission, every 2 s here) completes the onboarding
		masterAdmin().delete()
			.uri("/organizations/{org}/members/{user}", otherStudioId, userId)
			.retrieve()
			.toBodilessEntity();
		awaitOnboarded(tenant);
		assertThat(keycloakAdmin.organizationsOf(userId)).containsExactly(slug);
		assertThat(tenantManagement.findById(tenant.id()).orElseThrow().onboardingError()).isEmpty();
	}

	@Test
	void onboardingSucceedsAfterThePlatformOperatorCorrectedTheAdminEmail() throws Exception {
		// The e-mail address belongs to a user of another studio – the onboarding fails
		String otherStudio = uniqueSlug();
		String otherStudioId = keycloakAdmin.createOrganization(otherStudio, "Anderes Studio", true);
		String takenEmail = "vergeben@" + otherStudio + ".test";
		String otherUserId = keycloakAdmin.createUser(takenEmail, null, null, List.of()).id();
		keycloakAdmin.addMember(otherStudioId, otherUserId);

		String slug = uniqueSlug();
		Tenant tenant = tenantManagement.register(slug, "Fotostudio Korrektur",
				new InitialStudioAdmin(takenEmail, null, null));
		await().atMost(Duration.ofSeconds(30))
			.until(() -> tenantManagement.findById(tenant.id()).orElseThrow().onboardingError().isPresent());

		// The operator corrects the address and renames the studio ("Bearbeiten" in the platform area)
		String correctedEmail = "inhaber@" + slug + ".test";
		// 409 only if a resubmitted attempt (every 2 s here) records its failure at the same moment – then repeat
		await().atMost(Duration.ofSeconds(30))
			.until(() -> mockMvc
				.perform(patch("/api/platform/tenants/{id}", tenant.id().value()).with(TestTokens.platformAdmin())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"name": "Fotostudio Korrigiert", "adminEmail": "%s", "adminFirstName": "Ina", "adminLastName": "Haber"}"""
						.formatted(correctedEmail)))
				.andReturn()
				.getResponse()
				.getStatus() == 200);

		awaitOnboarded(tenant);
		Tenant onboarded = tenantManagement.findById(tenant.id()).orElseThrow();
		assertThat(onboarded.onboardingError()).isEmpty();
		// Data minimisation: the admin data is removed from the studio after onboarding
		assertThat(onboarded.initialAdmin()).isEmpty();

		KeycloakAdminClient.User admin = keycloakAdmin.findUserByEmail(correctedEmail).orElseThrow();
		assertThat(keycloakAdmin.organizationsOf(admin.id())).containsExactly(slug);
		assertThat(keycloakAdmin.organizationsOf(otherUserId)).containsExactly(otherStudio);
		assertThat(keycloakAdmin.findOrganization(slug).orElseThrow().description()).isEqualTo("Fotostudio Korrigiert");
		assertThat((String) awaitMailTo(correctedEmail).get("Text")).contains("Hallo Ina Haber,");
	}

	@Test
	void renamingTheStudioUpdatesTheOrganizationDescription() {
		String slug = uniqueSlug();
		Tenant tenant = tenantManagement.register(slug, "Fotostudio Alt",
				new InitialStudioAdmin("admin@" + slug + ".test", null, null));
		awaitOnboarded(tenant);

		tenantManagement.update(tenant.id(), "Fotostudio Neu", Optional.empty());

		await().atMost(Duration.ofSeconds(30))
			.until(() -> "Fotostudio Neu".equals(keycloakAdmin.findOrganization(slug).orElseThrow().description()));
		// The organization name stays the slug (unique in Keycloak)
		assertThat(keycloakAdmin.findOrganization(slug).orElseThrow().name()).isEqualTo(slug);
	}

	@Test
	void fetchesNewServiceAccountTokenWhenTheCachedOneIsRejected() {
		assertThat(keycloakAdmin.findOrganization("studio-a")).isPresent();
		// New signing keys (as after recreating the dev Keycloak): tokens issued so far are no longer accepted
		RestClient admin = masterAdmin();
		List<Map<String, Object>> keyProviders = admin.get()
			.uri("/components?type=org.keycloak.keys.KeyProvider")
			.retrieve()
			.body(new ParameterizedTypeReference<>() {
			});
		keyProviders.stream()
			.filter(provider -> "rsa-generated".equals(provider.get("providerId")))
			.forEach(provider -> admin.delete().uri("/components/{id}", provider.get("id")).retrieve().toBodilessEntity());

		assertThat(keycloakAdmin.findOrganization("studio-a")).isPresent();
	}

	private void awaitOnboarded(Tenant tenant) {
		await().atMost(Duration.ofSeconds(30))
			.until(() -> tenantManagement.findById(tenant.id()).orElseThrow().onboarded());
	}

	private static Map<String, Object> awaitMailTo(String email) {
		RestClient mailApi = RestClient.create("http://" + mailpit.getHost() + ":" + mailpit.getMappedPort(8025));
		String id = await().atMost(Duration.ofSeconds(30)).until(() -> {
			Map<String, Object> result = mailApi.get()
				.uri(uri -> uri.path("/api/v1/search").queryParam("query", "to:" + email).build())
				.retrieve()
				.body(new ParameterizedTypeReference<Map<String, Object>>() {
				});
			List<?> messages = (List<?>) result.get("messages");
			return messages.isEmpty() ? null : (String) ((Map<?, ?>) messages.getFirst()).get("ID");
		}, found -> found != null);
		return mailApi.get().uri("/api/v1/message/{id}", id).retrieve().body(new ParameterizedTypeReference<>() {
		});
	}

	/** What the user does via the e-mail link: set a password (and thereby verify the e-mail address). */
	private static void completeInvitation(String userId, String password) {
		RestClient admin = masterAdmin();
		admin.put()
			.uri("/users/{id}/reset-password", userId)
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("type", "password", "value", password, "temporary", false))
			.retrieve()
			.toBodilessEntity();
		admin.put()
			.uri("/users/{id}", userId)
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("emailVerified", true, "requiredActions", List.of()))
			.retrieve()
			.toBodilessEntity();
	}

	/** Keycloak's bootstrap admin, for steps outside the backend's service account (e.g. a user's own actions). */
	private static RestClient masterAdmin() {
		return RestClient.builder()
			.baseUrl(keycloakUrl() + "/admin/realms/photoffice")
			.defaultHeader("Authorization", "Bearer " + token("master", "admin-cli", "admin", "admin"))
			.build();
	}

	private static String accessToken(String username, String password) {
		return token("photoffice", "photoffice-dev-cli", username, password);
	}

	private static String token(String realm, String clientId, String username, String password) {
		var form = new LinkedMultiValueMap<String, String>();
		form.add("grant_type", "password");
		form.add("client_id", clientId);
		form.add("username", username);
		form.add("password", password);
		Map<?, ?> response = RestClient.create()
			.post()
			.uri(keycloakUrl() + "/realms/" + realm + "/protocol/openid-connect/token")
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(form)
			.retrieve()
			.body(Map.class);
		assertThat(response).isNotNull();
		return (String) response.get("access_token");
	}

	private static String keycloakUrl() {
		return "http://" + keycloak.getHost() + ":" + keycloak.getMappedPort(8080);
	}

	private static String uniqueSlug() {
		return "studio-" + UUID.randomUUID().toString().substring(0, 8);
	}

}
