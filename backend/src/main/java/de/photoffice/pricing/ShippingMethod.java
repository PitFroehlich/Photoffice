package de.photoffice.pricing;

import de.photoffice.tenant.TenantId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Shipping method with its cost, e.g. "Standardversand" or "Abholung im Studio". Belongs to exactly one tenant
 * (row-level security on table {@code shipping_method}).
 */
@Entity
@Table(name = "shipping_method")
public class ShippingMethod {

	@Id
	private UUID id;

	@Column(name = "tenant_id", nullable = false, updatable = false)
	private UUID tenantId;

	@Column(nullable = false)
	private String name;

	@Column(name = "price_cents", nullable = false)
	private int priceCents;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ShippingMethod() {
	}

	ShippingMethod(TenantId tenantId, ShippingMethodData data, Instant now) {
		this.id = UUID.randomUUID();
		this.tenantId = tenantId.value();
		this.createdAt = now;
		apply(data, now);
	}

	void apply(ShippingMethodData data, Instant now) {
		this.name = data.name();
		this.priceCents = data.priceCents();
		this.active = data.active();
		this.updatedAt = now;
	}

	public UUID id() {
		return id;
	}

	public TenantId tenantId() {
		return TenantId.of(tenantId);
	}

	public String name() {
		return name;
	}

	public int priceCents() {
		return priceCents;
	}

	public boolean active() {
		return active;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}

}
