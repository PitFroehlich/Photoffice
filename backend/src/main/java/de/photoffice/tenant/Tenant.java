package de.photoffice.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * A photo studio using the platform. All business data belongs to exactly one tenant.
 */
@Entity
@Table(name = "tenant")
public class Tenant {

	static final int MAX_ERROR_LENGTH = 1000;

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

	/** Set once the identity provider is set up for the studio; {@code null} while onboarding is pending. */
	@Column(name = "onboarded_at")
	private Instant onboardedAt;

	/** Reason of the last failed onboarding attempt (German, shown to the platform operator); cleared on success. */
	@Column(name = "onboarding_error")
	private String onboardingError;

	@Column(name = "onboarding_failed_at")
	private Instant onboardingFailedAt;

	/**
	 * First studio admin, kept only until onboarding completes (data minimisation); the onboarding reads the current
	 * values from here, so corrections by the platform operator are used by the next attempt.
	 */
	@Column(name = "admin_email")
	private String adminEmail;

	@Column(name = "admin_first_name")
	private String adminFirstName;

	@Column(name = "admin_last_name")
	private String adminLastName;

	/** Optimistic locking: editing and the onboarding must not overwrite each other's changes. */
	@Version
	private long version;

	protected Tenant() {
	}

	Tenant(String slug, String name, Instant createdAt) {
		this(slug, name, null, createdAt);
	}

	Tenant(String slug, String name, InitialStudioAdmin admin, Instant createdAt) {
		this.id = UUID.randomUUID();
		this.slug = slug;
		this.name = name;
		this.status = TenantStatus.ACTIVE;
		this.createdAt = createdAt;
		setInitialAdmin(admin);
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

	public boolean onboarded() {
		return onboardedAt != null;
	}

	public Optional<String> onboardingError() {
		return Optional.ofNullable(onboardingError);
	}

	public Optional<Instant> onboardingFailedAt() {
		return Optional.ofNullable(onboardingFailedAt);
	}

	/**
	 * First studio admin while onboarding is not complete; empty afterwards (and for studios registered before the
	 * data was stored at the studio, see {@link TenantRegistered#admin()}).
	 */
	public Optional<InitialStudioAdmin> initialAdmin() {
		return adminEmail == null ? Optional.empty()
				: Optional.of(new InitialStudioAdmin(adminEmail, adminFirstName, adminLastName));
	}

	/**
	 * @return {@code true} if the name changed
	 */
	boolean rename(String newName) {
		if (name.equals(newName)) {
			return false;
		}
		name = newName;
		return true;
	}

	void changeInitialAdmin(InitialStudioAdmin admin) {
		if (onboarded()) {
			throw new OnboardingCompletedException(this);
		}
		setInitialAdmin(admin);
	}

	private void setInitialAdmin(InitialStudioAdmin admin) {
		adminEmail = admin == null ? null : admin.email();
		adminFirstName = admin == null ? null : admin.firstName();
		adminLastName = admin == null ? null : admin.lastName();
	}

	/**
	 * @return {@code true} if the status changed
	 */
	boolean changeStatus(TenantStatus newStatus) {
		if (status == newStatus) {
			return false;
		}
		status = newStatus;
		return true;
	}

	void markOnboarded(Instant at) {
		if (onboardedAt == null) {
			onboardedAt = at;
		}
		onboardingError = null;
		onboardingFailedAt = null;
		setInitialAdmin(null);
	}

	/** Ignored once onboarding is complete (a late failure report of a duplicate delivery). */
	void recordOnboardingFailure(String reason, Instant at) {
		if (onboardedAt != null) {
			return;
		}
		onboardingError = reason.length() > MAX_ERROR_LENGTH ? reason.substring(0, MAX_ERROR_LENGTH - 1) + "…" : reason;
		onboardingFailedAt = at;
	}

}
