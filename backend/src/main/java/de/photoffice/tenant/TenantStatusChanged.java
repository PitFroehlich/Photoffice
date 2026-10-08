package de.photoffice.tenant;

/**
 * A studio was suspended or reactivated by the platform operator.
 */
public record TenantStatusChanged(TenantId tenantId, String slug, TenantStatus status) {
}
