package de.photoffice.identity;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Access to the Keycloak Admin REST API with the service account of client {@code clientId}.
 *
 * @param serverUrl Keycloak base URL (without {@code /realms/...})
 * @param realm realm of the studios
 * @param clientId confidential client with service account and {@code realm-management} roles
 * @param clientSecret secret of that client
 * @param invitation link in the invitation e-mail to the first studio admin
 */
@ConfigurationProperties("photoffice.keycloak")
record KeycloakAdminProperties(URI serverUrl, String realm, String clientId, String clientSecret,
		Invitation invitation) {

	/**
	 * @param clientId client the user is sent to after setting the password
	 * @param redirectUri where the user lands afterwards (must be a valid redirect URI of that client)
	 * @param lifespan how long the link in the e-mail is valid
	 */
	record Invitation(String clientId, URI redirectUri, Duration lifespan) {
	}

}
