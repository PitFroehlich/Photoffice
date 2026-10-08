package de.photoffice.pricing;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Queries need no tenant condition: row-level security only returns rows of the bound tenant.
 */
interface ShippingMethodRepository extends JpaRepository<ShippingMethod, UUID> {

	@Query("select s from ShippingMethod s order by s.priceCents, s.name")
	List<ShippingMethod> findAllOrdered();

	@Query("select count(s) > 0 from ShippingMethod s where lower(s.name) = lower(:name) and s.id <> :excludedId")
	boolean existsOtherWithName(@Param("name") String name, @Param("excludedId") UUID excludedId);

}
