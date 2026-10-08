package de.photoffice.gallery;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.UUID;

/**
 * Assignment of a customer to a gallery (table {@code gallery_customer}). Carries the tenant for row-level
 * security and the tenant-safe foreign keys.
 */
@Embeddable
class GalleryCustomerRef {

	@Column(name = "tenant_id", nullable = false)
	private UUID tenantId;

	@Column(name = "customer_id", nullable = false)
	private UUID customerId;

	protected GalleryCustomerRef() {
	}

	GalleryCustomerRef(UUID tenantId, UUID customerId) {
		this.tenantId = tenantId;
		this.customerId = customerId;
	}

	UUID customerId() {
		return customerId;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof GalleryCustomerRef ref && Objects.equals(customerId, ref.customerId)
				&& Objects.equals(tenantId, ref.tenantId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(tenantId, customerId);
	}

}
