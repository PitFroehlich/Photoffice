package de.photoffice.pricing;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Keyed by tenant id; row-level security additionally hides other tenants' rows.
 */
interface PriceListSettingsRepository extends JpaRepository<PriceListSettings, UUID> {

}
