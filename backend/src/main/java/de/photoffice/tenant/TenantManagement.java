package de.photoffice.tenant;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.FailedEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registers and looks up studios. Operations of the platform operator.
 */
@Service
@Transactional
public class TenantManagement {

	/** Lowercase letters, digits and inner hyphens, 3 to 63 characters (usable as subdomain). */
	static final Pattern SLUG_FORMAT = Pattern.compile("^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$");

	private final TenantRepository tenants;

	private final ApplicationEventPublisher events;

	private final FailedEventPublications failedPublications;

	private final Clock clock;

	TenantManagement(TenantRepository tenants, ApplicationEventPublisher events,
			FailedEventPublications failedPublications, Optional<Clock> clock) {
		this.tenants = tenants;
		this.events = events;
		this.failedPublications = failedPublications;
		this.clock = clock.orElse(Clock.systemUTC());
	}

	/**
	 * Registers a studio and publishes {@link TenantRegistered}; the studio is set up asynchronously (onboarding).
	 */
	public Tenant register(String slug, String name, InitialStudioAdmin admin) {
		if (!SLUG_FORMAT.matcher(slug).matches()) {
			throw new IllegalArgumentException("Ungültiges Kürzel „" + slug + "“: nur Kleinbuchstaben, Ziffern und Bindestriche, "
					+ "3 bis 63 Zeichen, nicht mit Bindestrich am Anfang oder Ende.");
		}
		if (tenants.existsBySlug(slug)) {
			throw new DuplicateTenantSlugException(slug);
		}
		Tenant tenant = tenants.save(new Tenant(slug, validName(name), admin, now()));
		// The admin data stays at the studio (correctable until onboarding completes), not in the event
		events.publishEvent(new TenantRegistered(tenant.id(), tenant.slug(), tenant.name()));
		return tenant;
	}

	/**
	 * Changes a studio. The name can always be changed ({@link TenantRenamed} updates the Keycloak organization),
	 * the first studio admin only until onboarding completes. The slug never changes.
	 *
	 * @param admin new data of the first studio admin; empty = unchanged
	 * @throws OnboardingCompletedException if the admin is to be changed after onboarding completed
	 */
	public Tenant update(TenantId id, String name, Optional<InitialStudioAdmin> admin) {
		Tenant tenant = require(id);
		admin.ifPresent(tenant::changeInitialAdmin);
		if (tenant.rename(validName(name))) {
			events.publishEvent(new TenantRenamed(tenant.id(), tenant.slug(), tenant.name()));
		}
		return tenant;
	}

	public Tenant suspend(TenantId id) {
		return changeStatus(id, TenantStatus.SUSPENDED);
	}

	public Tenant reactivate(TenantId id) {
		return changeStatus(id, TenantStatus.ACTIVE);
	}

	/**
	 * Records that the studio's onboarding is complete and clears a recorded failure. Repeated calls keep the first
	 * timestamp.
	 */
	public void markOnboarded(TenantId id) {
		require(id).markOnboarded(now());
	}

	/**
	 * Records why the onboarding failed, in its own transaction: the caller's transaction (the onboarding listener)
	 * is rolled back afterwards because the failure is rethrown so that the event publication is retried.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordOnboardingFailure(TenantId id, String reason) {
		require(id).recordOnboardingFailure(reason, now());
	}

	/**
	 * Delivers the failed {@link TenantRegistered} publication of this studio again right away instead of waiting for
	 * the periodic resubmission. Does nothing if there is no failed publication (e.g. already onboarded or a delivery
	 * still running). Runs without transaction: the listener completes the publication in its own transaction.
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public Tenant retryOnboarding(TenantId id) {
		Tenant tenant = require(id);
		if (!tenant.onboarded()) {
			failedPublications.resubmit(ResubmissionOptions.defaults()
				.withMinAge(Duration.ZERO)
				.withFilter(publication -> publication.getEvent() instanceof TenantRegistered registered
						&& registered.tenantId().equals(id)));
		}
		return tenant;
	}

	@Transactional(readOnly = true)
	public List<Tenant> findAll() {
		return tenants.findAllByOrderByNameAsc();
	}

	@Transactional(readOnly = true)
	public Optional<Tenant> findBySlug(String slug) {
		return tenants.findBySlug(slug);
	}

	@Transactional(readOnly = true)
	public Optional<Tenant> findById(TenantId id) {
		return tenants.findById(id.value());
	}

	private Tenant changeStatus(TenantId id, TenantStatus status) {
		Tenant tenant = require(id);
		if (tenant.changeStatus(status)) {
			events.publishEvent(new TenantStatusChanged(tenant.id(), tenant.slug(), status));
		}
		return tenant;
	}

	private static String validName(String name) {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Bitte einen Namen für das Studio angeben.");
		}
		return name.strip();
	}

	private Tenant require(TenantId id) {
		return tenants.findById(id.value()).orElseThrow(() -> new TenantNotFoundException(id));
	}

	/** PostgreSQL stores microseconds – truncate so returned values match the persisted ones. */
	private Instant now() {
		return clock.instant().truncatedTo(ChronoUnit.MICROS);
	}

}
