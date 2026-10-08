package de.photoffice.tenant;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
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

	private final Clock clock;

	TenantManagement(TenantRepository tenants, ApplicationEventPublisher events, Optional<Clock> clock) {
		this.tenants = tenants;
		this.events = events;
		this.clock = clock.orElse(Clock.systemUTC());
	}

	/**
	 * Registers a studio and publishes {@link TenantRegistered}; the studio is set up asynchronously (onboarding).
	 */
	public Tenant register(String slug, String name, InitialStudioAdmin admin) {
		if (!SLUG_FORMAT.matcher(slug).matches()) {
			throw new IllegalArgumentException("Invalid slug: " + slug);
		}
		if (tenants.existsBySlug(slug)) {
			throw new DuplicateTenantSlugException(slug);
		}
		Tenant tenant = tenants.save(new Tenant(slug, name.strip(), now()));
		events.publishEvent(new TenantRegistered(tenant.id(), tenant.slug(), tenant.name(), admin));
		return tenant;
	}

	public Tenant suspend(TenantId id) {
		return changeStatus(id, TenantStatus.SUSPENDED);
	}

	public Tenant reactivate(TenantId id) {
		return changeStatus(id, TenantStatus.ACTIVE);
	}

	/**
	 * Records that the studio's onboarding is complete. Repeated calls keep the first timestamp.
	 */
	public void markOnboarded(TenantId id) {
		require(id).markOnboarded(now());
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

	private Tenant require(TenantId id) {
		return tenants.findById(id.value()).orElseThrow(() -> new TenantNotFoundException(id));
	}

	/** PostgreSQL stores microseconds – truncate so returned values match the persisted ones. */
	private Instant now() {
		return clock.instant().truncatedTo(ChronoUnit.MICROS);
	}

}
