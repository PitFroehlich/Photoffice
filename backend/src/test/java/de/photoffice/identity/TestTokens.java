package de.photoffice.identity;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.util.List;
import java.util.Map;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

/**
 * Keycloak-like access tokens for MockMvc tests.
 */
public final class TestTokens {

	private TestTokens() {
	}

	public static JwtRequestPostProcessor platformAdmin() {
		return token("operator", List.of(), "platform-admin");
	}

	public static JwtRequestPostProcessor studioAdmin(String studioSlug) {
		return token("admin-" + studioSlug, List.of(studioSlug), "studio-admin");
	}

	public static JwtRequestPostProcessor photographer(String studioSlug) {
		return token("foto-" + studioSlug, List.of(studioSlug), "photographer");
	}

	public static JwtRequestPostProcessor token(String username, List<String> organizations, String... realmRoles) {
		return jwt().jwt(jwt -> jwt.subject(username)
			.claim("preferred_username", username)
			.claim("email", username + "@example.test")
			.claim("organization", organizations)
			.claim("realm_access", Map.of("roles", List.of(realmRoles))))
			.authorities(new KeycloakRealmRoleConverter());
	}

}
