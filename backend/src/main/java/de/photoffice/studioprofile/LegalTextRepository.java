package de.photoffice.studioprofile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Queries need no tenant condition: row-level security only returns rows of the bound tenant.
 */
interface LegalTextRepository extends JpaRepository<LegalText, UUID> {

	List<LegalText> findAllBy();

	Optional<LegalText> findByKind(LegalTextKind kind);

}
