package de.photoffice.customer;

import de.photoffice.tenant.TenantId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * End customer of a studio. Belongs to exactly one tenant (row-level security on table {@code customer}).
 */
@Entity
@Table(name = "customer")
public class Customer {

	@Id
	private UUID id;

	@Column(name = "tenant_id", nullable = false, updatable = false)
	private UUID tenantId;

	@Column(name = "first_name", nullable = false)
	private String firstName;

	@Column(name = "last_name", nullable = false)
	private String lastName;

	@Column(nullable = false)
	private String email;

	private String phone;

	private String street;

	@Column(name = "postal_code")
	private String postalCode;

	private String city;

	private String notes;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Customer() {
	}

	Customer(TenantId tenantId, CustomerData data, Instant now) {
		this.id = UUID.randomUUID();
		this.tenantId = tenantId.value();
		this.createdAt = now;
		apply(data, now);
	}

	void apply(CustomerData data, Instant now) {
		this.firstName = data.firstName();
		this.lastName = data.lastName();
		this.email = data.email();
		this.phone = data.phone();
		this.street = data.street();
		this.postalCode = data.postalCode();
		this.city = data.city();
		this.notes = data.notes();
		this.updatedAt = now;
	}

	public UUID id() {
		return id;
	}

	public TenantId tenantId() {
		return TenantId.of(tenantId);
	}

	public String firstName() {
		return firstName;
	}

	public String lastName() {
		return lastName;
	}

	public String email() {
		return email;
	}

	public String phone() {
		return phone;
	}

	public String street() {
		return street;
	}

	public String postalCode() {
		return postalCode;
	}

	public String city() {
		return city;
	}

	public String notes() {
		return notes;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}

}
