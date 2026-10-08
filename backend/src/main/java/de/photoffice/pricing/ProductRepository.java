package de.photoffice.pricing;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Queries need no tenant condition: row-level security only returns rows of the bound tenant.
 */
interface ProductRepository extends JpaRepository<Product, UUID> {

	@Query("""
			select p from Product p
			order by p.type desc, p.priceCents, p.printFormat, p.paperType, p.downloadName
			""")
	List<Product> findAllOrdered();

	@Query("""
			select count(p) > 0 from Product p
			where p.type = de.photoffice.pricing.ProductType.PRINT
			  and lower(p.paperType) = lower(:paperType) and lower(p.printFormat) = lower(:printFormat)
			  and p.id <> :excludedId
			""")
	boolean existsOtherPrint(@Param("paperType") String paperType, @Param("printFormat") String printFormat,
			@Param("excludedId") UUID excludedId);

	@Query("""
			select count(p) > 0 from Product p
			where p.type = de.photoffice.pricing.ProductType.DOWNLOAD and lower(p.downloadName) = lower(:name)
			  and p.id <> :excludedId
			""")
	boolean existsOtherDownload(@Param("name") String name, @Param("excludedId") UUID excludedId);

}
