package de.photoffice.tenant;

/**
 * A studio was registered. Listeners set up everything else the studio needs (e.g. the Keycloak organization
 * and the first studio admin). Published within the registering transaction, delivered after commit.
 */
public record TenantRegistered(TenantId tenantId, String slug, String name, InitialStudioAdmin admin) {
}
