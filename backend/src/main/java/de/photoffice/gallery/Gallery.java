package de.photoffice.gallery;

import de.photoffice.tenant.TenantId;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A gallery of a studio: photos (#9) for one or more customers, visible to them while online and not expired.
 * Belongs to exactly one tenant (row-level security on {@code gallery} and {@code gallery_customer}).
 */
@Entity
@Table(name = "gallery")
public class Gallery {

	@Id
	private UUID id;

	@Column(name = "tenant_id", nullable = false, updatable = false)
	private UUID tenantId;

	@Column(nullable = false)
	private String name;

	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private GalleryStatus status;

	@Column(name = "expires_on")
	private LocalDate expiresOn;

	@Column(name = "published_at")
	private Instant publishedAt;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "gallery_customer", joinColumns = @JoinColumn(name = "gallery_id"))
	private Set<GalleryCustomerRef> customers = new HashSet<>();

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Gallery() {
	}

	Gallery(TenantId tenantId, GalleryData data, Instant now) {
		this.id = UUID.randomUUID();
		this.tenantId = tenantId.value();
		this.status = GalleryStatus.DRAFT;
		this.createdAt = now;
		apply(data, now);
	}

	void apply(GalleryData data, Instant now) {
		this.name = data.name();
		this.description = data.description();
		this.expiresOn = data.expiresOn();
		this.customers.clear();
		data.customerIds().forEach(customerId -> customers.add(new GalleryCustomerRef(tenantId, customerId)));
		this.updatedAt = now;
	}

	void publish(Instant now) {
		this.status = GalleryStatus.ONLINE;
		this.publishedAt = now;
		this.updatedAt = now;
	}

	void unpublish(Instant now) {
		this.status = GalleryStatus.OFFLINE;
		this.updatedAt = now;
	}

	/** The last day customers have access has passed. */
	public boolean isExpired(LocalDate today) {
		return expiresOn != null && expiresOn.isBefore(today);
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

	public String description() {
		return description;
	}

	public GalleryStatus status() {
		return status;
	}

	public LocalDate expiresOn() {
		return expiresOn;
	}

	public Instant publishedAt() {
		return publishedAt;
	}

	public Set<UUID> customerIds() {
		return customers.stream().map(GalleryCustomerRef::customerId).collect(Collectors.toUnmodifiableSet());
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}

}
