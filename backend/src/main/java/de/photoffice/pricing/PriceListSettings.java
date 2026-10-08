package de.photoffice.pricing;

import de.photoffice.tenant.TenantId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/**
 * Currency and VAT rate of a studio's price list. All prices are gross prices (including VAT) for end
 * customers. At most one row per tenant; without a row the defaults apply.
 */
@Entity
@Table(name = "price_list_settings")
public class PriceListSettings {

	public static final String DEFAULT_CURRENCY = "EUR";

	public static final BigDecimal DEFAULT_VAT_RATE_PERCENT = new BigDecimal("19.00");

	@Id
	@Column(name = "tenant_id")
	private UUID tenantId;

	@Column(nullable = false)
	private String currency;

	@Column(name = "vat_rate_percent", nullable = false, precision = 4, scale = 2)
	private BigDecimal vatRatePercent;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected PriceListSettings() {
	}

	PriceListSettings(TenantId tenantId, Instant now) {
		this.tenantId = tenantId.value();
		this.currency = DEFAULT_CURRENCY;
		this.vatRatePercent = DEFAULT_VAT_RATE_PERCENT;
		this.updatedAt = now;
	}

	void changeVatRate(BigDecimal percent, Instant now) {
		this.vatRatePercent = validVatRate(percent);
		this.updatedAt = now;
	}

	/** 0 ≤ rate < 100 with at most two decimal places, stored with scale 2. */
	static BigDecimal validVatRate(BigDecimal percent) {
		if (percent == null || percent.signum() < 0 || percent.compareTo(new BigDecimal("100")) >= 0
				|| percent.stripTrailingZeros().scale() > 2) {
			throw new IllegalArgumentException(
					"Der Steuersatz muss zwischen 0 und 99,99 % liegen (höchstens zwei Nachkommastellen).");
		}
		return percent.setScale(2, RoundingMode.UNNECESSARY);
	}

	public TenantId tenantId() {
		return TenantId.of(tenantId);
	}

	public String currency() {
		return currency;
	}

	public BigDecimal vatRatePercent() {
		return vatRatePercent;
	}

}
