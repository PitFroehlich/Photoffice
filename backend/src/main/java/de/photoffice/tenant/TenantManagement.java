package de.photoffice.tenant;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
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

	private final Clock clock;

	TenantManagement(TenantRepository tenants, Optional<Clock> clock) {
		this.tenants = tenants;
		this.clock = clock.orElse(Clock.systemUTC());
	}

	public Tenant register(String slug, String name) {
		if (!SLUG_FORMAT.matcher(slug).matches()) {
			throw new IllegalArgumentException("Invalid slug: " + slug);
		}
		if (tenants.existsBySlug(slug)) {
			throw new DuplicateTenantSlugException(slug);
		}
		// PostgreSQL stores microseconds – truncate so the returned value matches the persisted one
		return tenants.save(new Tenant(slug, name.strip(), clock.instant().truncatedTo(ChronoUnit.MICROS)));
	}

	@Transactional(readOnly = true)
	public List<Tenant> findAll() {
		return tenants.findAllByOrderByNameAsc();
	}

	@Transactional(readOnly = true)
	public Optional<Tenant> findById(TenantId id) {
		return tenants.findById(id.value());
	}

}
