package de.photoffice.tenant;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface TenantRepository extends JpaRepository<Tenant, UUID>, JpaSpecificationExecutor<Tenant> {

	boolean existsBySlug(String slug);

	Optional<Tenant> findBySlug(String slug);

	/** Name, slug or e-mail of the first admin contain the term (case-insensitive). */
	static Specification<Tenant> matches(String term) {
		String pattern = likePattern(term);
		return (tenant, query, cb) -> cb.or(cb.like(cb.lower(tenant.get("name")), pattern, '\\'),
				cb.like(cb.lower(tenant.get("slug")), pattern, '\\'),
				cb.like(cb.lower(tenant.get("adminEmail")), pattern, '\\'));
	}

	static Specification<Tenant> hasStatus(TenantStatus status) {
		return (tenant, query, cb) -> cb.equal(tenant.get("status"), status);
	}

	static Specification<Tenant> onboarding(OnboardingFilter filter) {
		return switch (filter) {
			case PENDING -> (tenant, query, cb) -> cb.isNull(tenant.get("onboardedAt"));
			case FAILED -> (tenant, query, cb) -> cb.and(cb.isNull(tenant.get("onboardedAt")),
					cb.isNotNull(tenant.get("onboardingError")));
			case COMPLETED -> (tenant, query, cb) -> cb.isNotNull(tenant.get("onboardedAt"));
		};
	}

	/** {@code %term%} with LIKE wildcards in the term escaped. */
	static String likePattern(String term) {
		String escaped = term.strip()
			.toLowerCase(Locale.ROOT)
			.replace("\\", "\\\\")
			.replace("%", "\\%")
			.replace("_", "\\_");
		return "%" + escaped + "%";
	}

}
