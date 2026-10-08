package de.photoffice.gallery;

import jakarta.persistence.criteria.JoinType;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Queries need no tenant condition: row-level security only returns rows of the bound tenant.
 */
interface GalleryRepository extends JpaRepository<Gallery, UUID>, JpaSpecificationExecutor<Gallery> {

	static Specification<Gallery> nameContains(String likePattern) {
		return (root, query, cb) -> cb.like(cb.lower(root.get("name")), likePattern, '\\');
	}

	static Specification<Gallery> hasStatus(GalleryStatus status) {
		return (root, query, cb) -> cb.equal(root.get("status"), status);
	}

	static Specification<Gallery> hasCustomer(UUID customerId) {
		return (root, query, cb) -> {
			query.distinct(true);
			return cb.equal(root.join("customers", JoinType.INNER).get("customerId"), customerId);
		};
	}

}
