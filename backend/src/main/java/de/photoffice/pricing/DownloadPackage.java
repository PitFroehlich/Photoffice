package de.photoffice.pricing;

import de.photoffice.tenant.TenantId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Package price for downloads of one gallery: a number of images ("10 Downloads") or the whole gallery.
 * Belongs to exactly one tenant (row-level security on table {@code download_package}).
 */
@Entity
@Table(name = "download_package")
public class DownloadPackage {

	@Id
	private UUID id;

	@Column(name = "tenant_id", nullable = false, updatable = false)
	private UUID tenantId;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DownloadPackageKind kind;

	@Column(name = "image_count")
	private Integer imageCount;

	@Column(name = "download_product_id", nullable = false)
	private UUID downloadProductId;

	@Column(name = "price_cents", nullable = false)
	private int priceCents;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected DownloadPackage() {
	}

	DownloadPackage(TenantId tenantId, DownloadPackageData data, Instant now) {
		this.id = UUID.randomUUID();
		this.tenantId = tenantId.value();
		this.createdAt = now;
		apply(data, now);
	}

	void apply(DownloadPackageData data, Instant now) {
		this.name = data.name();
		this.kind = data.kind();
		this.imageCount = data.imageCount();
		this.downloadProductId = data.downloadProductId();
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

	public DownloadPackageKind kind() {
		return kind;
	}

	/** Number of images for {@link DownloadPackageKind#IMAGE_COUNT}, otherwise {@code null}. */
	public Integer imageCount() {
		return imageCount;
	}

	/** The download variant the images are delivered in. */
	public UUID downloadProductId() {
		return downloadProductId;
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
