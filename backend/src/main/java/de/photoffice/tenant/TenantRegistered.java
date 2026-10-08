package de.photoffice.tenant;

/**
 * A studio was registered. Listeners set up everything else the studio needs (e.g. the Keycloak organization
 * and the first studio admin). Published within the registering transaction, delivered after commit.
 *
 * @param admin only set in publications from before issue #42 (they may still be pending in
 * {@code event_publication}); new events carry {@code null} – the current data of the first admin is
 * {@link Tenant#initialAdmin()}, which the platform operator can correct until onboarding completes (ADR 0012)
 */
public record TenantRegistered(TenantId tenantId, String slug, String name, InitialStudioAdmin admin) {

	public TenantRegistered(TenantId tenantId, String slug, String name) {
		this(tenantId, slug, name, null);
	}

}
