package de.photoffice.customer;

import de.photoffice.tenant.TenantId;
import java.util.UUID;

/**
 * Published when a customer was deleted, e.g. so that gallery assignments can be removed (#8).
 */
public record CustomerDeleted(TenantId tenantId, UUID customerId) {
}
