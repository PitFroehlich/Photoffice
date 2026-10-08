package de.photoffice.studioprofile;

import de.photoffice.tenant.TenantId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Master data of a studio (name, address, contact, tax and bank details) that customers see later in the gallery,
 * at checkout and in the imprint. At most one row per tenant (row-level security on table {@code studio_profile}).
 */
@Entity
@Table(name = "studio_profile")
public class StudioProfile {

	@Id
	@Column(name = "tenant_id")
	private UUID tenantId;

	@Column(name = "display_name", nullable = false)
	private String displayName;

	private String street;

	@Column(name = "postal_code")
	private String postalCode;

	private String city;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Country country;

	private String email;

	private String phone;

	private String website;

	@Column(name = "tax_number")
	private String taxNumber;

	@Column(name = "vat_id")
	private String vatId;

	@Column(name = "account_holder")
	private String accountHolder;

	private String iban;

	private String bic;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected StudioProfile() {
	}

	/** Profile of a studio that has not saved one yet: studio name as display name, Germany, not persisted. */
	static StudioProfile empty(TenantId tenantId, String studioName) {
		StudioProfile profile = new StudioProfile();
		profile.tenantId = tenantId.value();
		profile.displayName = studioName;
		profile.country = Country.DE;
		return profile;
	}

	void apply(StudioProfileData data, Instant now) {
		this.displayName = data.displayName();
		this.street = data.street();
		this.postalCode = data.postalCode();
		this.city = data.city();
		this.country = data.country();
		this.email = data.email();
		this.phone = data.phone();
		this.website = data.website();
		this.taxNumber = data.taxNumber();
		this.vatId = data.vatId();
		this.accountHolder = data.accountHolder();
		this.iban = data.iban();
		this.bic = data.bic();
		this.updatedAt = now;
	}

	public TenantId tenantId() {
		return TenantId.of(tenantId);
	}

	public String displayName() {
		return displayName;
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

	public Country country() {
		return country;
	}

	public String email() {
		return email;
	}

	public String phone() {
		return phone;
	}

	public String website() {
		return website;
	}

	public String taxNumber() {
		return taxNumber;
	}

	public String vatId() {
		return vatId;
	}

	public String accountHolder() {
		return accountHolder;
	}

	public String iban() {
		return iban;
	}

	public String bic() {
		return bic;
	}

	/** Empty as long as the studio has never saved its profile. */
	public Optional<Instant> updatedAt() {
		return Optional.ofNullable(updatedAt);
	}

}
