package de.photoffice.identity;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Minimal client for the Keycloak Admin REST API (organizations, users, realm roles), authenticated with the
 * client credentials of the backend's service account. Only what studio onboarding needs.
 */
class KeycloakAdminClient {

	private static final ParameterizedTypeReference<List<Organization>> ORGANIZATIONS = new ParameterizedTypeReference<>() {
	};

	private static final ParameterizedTypeReference<List<User>> USERS = new ParameterizedTypeReference<>() {
	};

	private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT = new ParameterizedTypeReference<>() {
	};

	private final KeycloakAdminProperties properties;

	private final RestClient admin;

	private final RestClient tokenEndpoint;

	private final Clock clock;

	private AccessToken accessToken;

	KeycloakAdminClient(KeycloakAdminProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
		this.tokenEndpoint = RestClient.create(properties.serverUrl() + "/realms/" + properties.realm());
		this.admin = RestClient.builder()
			.baseUrl(properties.serverUrl() + "/admin/realms/" + properties.realm())
			.requestInterceptor(bearerToken())
			.build();
	}

	record Organization(String id, String alias, String name, String description, boolean enabled) {
	}

	record User(String id, String username, String email, List<String> requiredActions) {

		boolean hasRequiredAction(String action) {
			return requiredActions != null && requiredActions.contains(action);
		}

	}

	Optional<Organization> findOrganization(String alias) {
		List<Organization> found = admin.get()
			.uri(uri -> uri.path("/organizations").queryParam("q", "alias:" + alias).build())
			.retrieve()
			.body(ORGANIZATIONS);
		return found == null ? Optional.empty() : found.stream().filter(o -> alias.equals(o.alias())).findFirst();
	}

	/**
	 * Keycloak requires unique organization names, studio names need not be unique. The organization is therefore
	 * named like its alias (the slug); the studio name goes into the description.
	 *
	 * @return id of the new organization
	 */
	String createOrganization(String alias, String studioName, boolean enabled) {
		URI location = admin.post()
			.uri("/organizations")
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("alias", alias, "name", alias, "description", studioName, "enabled", enabled))
			.retrieve()
			.toBodilessEntity()
			.getHeaders()
			.getLocation();
		return lastPathSegment(location);
	}

	void setOrganizationEnabled(String organizationId, boolean enabled) {
		updateOrganization(organizationId, Map.of("enabled", enabled));
	}

	/** The studio name is the organization's description (see {@link #createOrganization}). */
	void setOrganizationDescription(String organizationId, String studioName) {
		updateOrganization(organizationId, Map.of("description", studioName));
	}

	/**
	 * Sets the given fields; does nothing if they already have these values.
	 */
	private void updateOrganization(String organizationId, Map<String, Object> changes) {
		// PUT replaces the organization – read the full representation to keep domains and attributes
		Map<String, Object> organization = new HashMap<>(
				admin.get().uri("/organizations/{id}", organizationId).retrieve().body(JSON_OBJECT));
		if (changes.entrySet().stream().allMatch(change -> change.getValue().equals(organization.get(change.getKey())))) {
			return;
		}
		organization.putAll(changes);
		admin.put()
			.uri("/organizations/{id}", organizationId)
			.contentType(MediaType.APPLICATION_JSON)
			.body(organization)
			.retrieve()
			.toBodilessEntity();
	}

	Optional<User> findUserByEmail(String email) {
		List<User> found = admin.get()
			.uri(uri -> uri.path("/users").queryParam("email", email).queryParam("exact", true).build())
			.retrieve()
			.body(USERS);
		return found == null ? Optional.empty()
				: found.stream().filter(u -> email.equalsIgnoreCase(u.email())).findFirst();
	}

	/**
	 * Creates an enabled user without credentials; the user sets the password via the invitation e-mail.
	 */
	User createUser(String email, String firstName, String lastName, List<String> requiredActions) {
		Map<String, Object> user = new HashMap<>();
		user.put("username", email);
		user.put("email", email);
		user.put("firstName", firstName);
		user.put("lastName", lastName);
		user.put("enabled", true);
		user.put("requiredActions", requiredActions);
		URI location = admin.post()
			.uri("/users")
			.contentType(MediaType.APPLICATION_JSON)
			.body(user)
			.retrieve()
			.toBodilessEntity()
			.getHeaders()
			.getLocation();
		return new User(lastPathSegment(location), email, email, requiredActions);
	}

	/**
	 * Aliases of the organizations the user belongs to.
	 */
	List<String> organizationsOf(String userId) {
		List<Organization> organizations = admin.get()
			.uri("/organizations/members/{userId}/organizations", userId)
			.retrieve()
			.body(ORGANIZATIONS);
		return organizations == null ? List.of() : organizations.stream().map(Organization::alias).toList();
	}

	/**
	 * Adds the user to the organization; does nothing if the user already is a member.
	 */
	void addMember(String organizationId, String userId) {
		admin.post()
			.uri("/organizations/{id}/members", organizationId)
			.contentType(MediaType.APPLICATION_JSON)
			.body("\"" + userId + "\"")
			.retrieve()
			.onStatus(status -> status.isSameCodeAs(HttpStatus.CONFLICT), (request, response) -> {
			})
			.toBodilessEntity();
	}

	/**
	 * Grants a realm role; granting an already assigned role has no effect.
	 */
	void assignRealmRole(String userId, String roleName) {
		Map<String, Object> role = admin.get().uri("/roles/{name}", roleName).retrieve().body(JSON_OBJECT);
		admin.post()
			.uri("/users/{id}/role-mappings/realm", userId)
			.contentType(MediaType.APPLICATION_JSON)
			.body(List.of(role))
			.retrieve()
			.toBodilessEntity();
	}

	/**
	 * Sends Keycloak's "update your account" e-mail with a link to perform the given required actions.
	 */
	void sendActionsEmail(String userId, List<String> actions) {
		KeycloakAdminProperties.Invitation invitation = properties.invitation();
		admin.put()
			.uri(uri -> uri.path("/users/{id}/execute-actions-email")
				.queryParam("client_id", invitation.clientId())
				.queryParam("redirect_uri", invitation.redirectUri())
				.queryParam("lifespan", invitation.lifespan().toSeconds())
				.build(userId))
			.contentType(MediaType.APPLICATION_JSON)
			.body(actions)
			.retrieve()
			.toBodilessEntity();
	}

	/**
	 * Adds the service account token. A 401 means the cached token is no longer accepted (e.g. Keycloak was
	 * restarted with new keys): fetch a new token and try once more.
	 */
	private ClientHttpRequestInterceptor bearerToken() {
		return (request, body, execution) -> {
			request.getHeaders().setBearerAuth(currentAccessToken());
			ClientHttpResponse response = execution.execute(request, body);
			if (!response.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
				return response;
			}
			response.close();
			discardAccessToken();
			request.getHeaders().setBearerAuth(currentAccessToken());
			return execution.execute(request, body);
		};
	}

	private synchronized void discardAccessToken() {
		accessToken = null;
	}

	private synchronized String currentAccessToken() {
		Instant now = clock.instant();
		if (accessToken == null || accessToken.isExpiredAt(now.plusSeconds(30))) {
			accessToken = requestAccessToken(now);
		}
		return accessToken.value();
	}

	private AccessToken requestAccessToken(Instant now) {
		var form = new LinkedMultiValueMap<String, String>();
		form.add("grant_type", "client_credentials");
		form.add("client_id", properties.clientId());
		form.add("client_secret", properties.clientSecret());
		Map<String, Object> response = tokenEndpoint.post()
			.uri("/protocol/openid-connect/token")
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
			.body(form)
			.retrieve()
			.body(JSON_OBJECT);
		if (response == null || !(response.get("access_token") instanceof String token)) {
			throw new IllegalStateException("Keycloak token endpoint returned no access token");
		}
		long expiresIn = response.get("expires_in") instanceof Number seconds ? seconds.longValue() : 60;
		return new AccessToken(token, now.plusSeconds(expiresIn));
	}

	private static String lastPathSegment(URI location) {
		if (location == null) {
			throw new IllegalStateException("Keycloak response without Location header");
		}
		String path = location.getPath();
		return path.substring(path.lastIndexOf('/') + 1);
	}

	private record AccessToken(String value, Instant expiresAt) {

		boolean isExpiredAt(Instant instant) {
			return !instant.isBefore(expiresAt);
		}

	}

}
