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
 * Product sold individually at a single price, e.g. a print "matt, 13 × 18 cm" or a download variant
 * "Web 2048 px". Belongs to exactly one tenant (row-level security on table {@code product}).
 */
@Entity
@Table(name = "product")
public class Product {

	@Id
	private UUID id;

	@Column(name = "tenant_id", nullable = false, updatable = false)
	private UUID tenantId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private ProductType type;

	@Column(name = "paper_type")
	private String paperType;

	@Column(name = "print_format")
	private String printFormat;

	@Column(name = "download_name")
	private String downloadName;

	@Column(name = "max_edge_px")
	private Integer maxEdgePx;

	@Column(name = "price_cents", nullable = false)
	private int priceCents;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Product() {
	}

	Product(TenantId tenantId, ProductData data, Instant now) {
		this.id = UUID.randomUUID();
		this.tenantId = tenantId.value();
		this.type = data.type();
		this.createdAt = now;
		apply(data, now);
	}

	void apply(ProductData data, Instant now) {
		if (data.type() != type) {
			throw new IllegalArgumentException("Der Produkttyp kann nicht geändert werden.");
		}
		this.paperType = data.paperType();
		this.printFormat = data.printFormat();
		this.downloadName = data.downloadName();
		this.maxEdgePx = data.maxEdgePx();
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

	public ProductType type() {
		return type;
	}

	public String paperType() {
		return paperType;
	}

	public String printFormat() {
		return printFormat;
	}

	/** Name of a download variant, e.g. "Web 2048 px" ({@code null} for prints). */
	public String downloadName() {
		return downloadName;
	}

	/** Maximum edge length in pixels of a download; {@code null} = original size (or a print). */
	public Integer maxEdgePx() {
		return maxEdgePx;
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
