package de.photoffice.tenant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface TenantRepository extends JpaRepository<Tenant, UUID> {

	boolean existsBySlug(String slug);

	Optional<Tenant> findBySlug(String slug);

	List<Tenant> findAllByOrderByNameAsc();

}
