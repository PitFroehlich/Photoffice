package de.photoffice.customer;

import de.photoffice.tenant.TenantContext;
import de.photoffice.tenant.TenantId;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Customers of the current studio ({@link TenantContext}).
 */
@Service
@Transactional
public class CustomerManagement {

	private static final Sort BY_NAME = Sort.by("lastName", "firstName", "email");

	private final CustomerRepository customers;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	CustomerManagement(CustomerRepository customers, ApplicationEventPublisher events, Optional<Clock> clock) {
		this.customers = customers;
		this.events = events;
		this.clock = clock.orElse(Clock.systemUTC());
	}

	@Transactional(readOnly = true)
	public Page<Customer> search(String term, int page, int size) {
		PageRequest pageRequest = PageRequest.of(page, size, BY_NAME);
		if (term == null || term.isBlank()) {
			return customers.findAll(pageRequest);
		}
		return customers.search(likePattern(term), pageRequest);
	}

	@Transactional(readOnly = true)
	public Customer get(UUID id) {
		return customers.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
	}

	/** Customers of the current studio with the given ids; ids of other studios are simply not found (RLS). */
	@Transactional(readOnly = true)
	public List<Customer> findAllById(Collection<UUID> ids) {
		return ids.isEmpty() ? List.of() : customers.findAllById(ids);
	}

	public Customer create(CustomerData data) {
		TenantId tenant = TenantContext.require();
		ensureEmailIsFree(data.email(), null);
		return saveChecked(new Customer(tenant, data, now()), data.email());
	}

	public Customer update(UUID id, CustomerData data) {
		Customer customer = get(id);
		ensureEmailIsFree(data.email(), id);
		customer.apply(data, now());
		return saveChecked(customer, data.email());
	}

	public void delete(UUID id) {
		Customer customer = get(id);
		customers.delete(customer);
		events.publishEvent(new CustomerDeleted(customer.tenantId(), customer.id()));
	}

	private void ensureEmailIsFree(String email, UUID excludedId) {
		if (customers.existsOtherWithEmail(email, excludedId == null ? new UUID(0, 0) : excludedId)) {
			throw new DuplicateCustomerEmailException(email);
		}
	}

	/** The unique index decides in case of concurrent requests. */
	private Customer saveChecked(Customer customer, String email) {
		try {
			return customers.saveAndFlush(customer);
		}
		catch (DataIntegrityViolationException ex) {
			throw new DuplicateCustomerEmailException(email);
		}
	}

	private Instant now() {
		return clock.instant().truncatedTo(ChronoUnit.MICROS);
	}

	/** Case-insensitive "contains" with LIKE wildcards of the user input escaped. */
	static String likePattern(String term) {
		String escaped = term.strip()
			.toLowerCase()
			.replace("\\", "\\\\")
			.replace("%", "\\%")
			.replace("_", "\\_");
		return "%" + escaped + "%";
	}

}
