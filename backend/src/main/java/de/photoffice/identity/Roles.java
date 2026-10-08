package de.photoffice.identity;

/**
 * Spring Security role names (without {@code ROLE_} prefix), derived from Keycloak realm roles
 * ({@code platform-admin} → {@code PLATFORM_ADMIN}).
 */
public final class Roles {

	public static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";

	public static final String STUDIO_ADMIN = "STUDIO_ADMIN";

	public static final String PHOTOGRAPHER = "PHOTOGRAPHER";

	private Roles() {
	}

}
