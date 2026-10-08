package de.photoffice.tenant;

/**
 * The platform operator changed the name of a studio (the slug never changes). Deliveries may be repeated or arrive
 * out of order – listeners should use the current name from {@link TenantManagement#findById(TenantId)}.
 */
public record TenantRenamed(TenantId tenantId, String slug, String name) {
}
