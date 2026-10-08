package de.photoffice.customer;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Queries need no tenant condition: row-level security only returns rows of the bound tenant.
 */
interface CustomerRepository extends JpaRepository<Customer, UUID> {

	@Query("""
			select c from Customer c
			where lower(c.firstName) like :pattern escape '\\'
			   or lower(c.lastName) like :pattern escape '\\'
			   or lower(c.email) like :pattern escape '\\'
			   or lower(c.city) like :pattern escape '\\'
			""")
	Page<Customer> search(@Param("pattern") String pattern, Pageable pageable);

	@Query("select count(c) > 0 from Customer c where lower(c.email) = lower(:email) and c.id <> :excludedId")
	boolean existsOtherWithEmail(@Param("email") String email, @Param("excludedId") UUID excludedId);

}
