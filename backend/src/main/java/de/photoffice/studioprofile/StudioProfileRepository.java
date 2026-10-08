package de.photoffice.studioprofile;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Keyed by tenant id; row-level security additionally hides other tenants' rows.
 */
interface StudioProfileRepository extends JpaRepository<StudioProfile, UUID> {

}
