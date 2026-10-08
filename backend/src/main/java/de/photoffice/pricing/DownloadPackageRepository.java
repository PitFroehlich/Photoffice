package de.photoffice.pricing;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Queries need no tenant condition: row-level security only returns rows of the bound tenant.
 */
interface DownloadPackageRepository extends JpaRepository<DownloadPackage, UUID> {

	@Query("select d from DownloadPackage d order by d.priceCents, d.name")
	List<DownloadPackage> findAllOrdered();

	@Query("select count(d) > 0 from DownloadPackage d where lower(d.name) = lower(:name) and d.id <> :excludedId")
	boolean existsOtherWithName(@Param("name") String name, @Param("excludedId") UUID excludedId);

}
