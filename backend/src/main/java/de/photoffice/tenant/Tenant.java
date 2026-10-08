package de.photoffice.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A photo studio using the platform. All business data belongs to exactly one tenant.
 */
@Entity
@Table(name = "tenant")
public class Tenant {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true)
	private String slug;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TenantStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Tenant() {
	}

	Tenant(String slug, String name, Instant createdAt) {
		this.id = UUID.randomUUID();
		this.slug = slug;
		this.name = name;
		this.status = TenantStatus.ACTIVE;
		this.createdAt = createdAt;
	}

	public TenantId id() {
		return TenantId.of(id);
	}

	public String slug() {
		return slug;
	}

	public String name() {
		return name;
	}

	public TenantStatus status() {
		return status;
	}

	public Instant createdAt() {
		return createdAt;
	}

}
