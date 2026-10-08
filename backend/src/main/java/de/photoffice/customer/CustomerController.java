package de.photoffice.customer;

import de.photoffice.api.CustomersApi;
import de.photoffice.api.model.CustomerInput;
import de.photoffice.api.model.CustomerPage;
import java.net.URI;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class CustomerController implements CustomersApi {

	private final CustomerManagement customerManagement;

	CustomerController(CustomerManagement customerManagement) {
		this.customerManagement = customerManagement;
	}

	@Override
	public ResponseEntity<CustomerPage> listCustomers(String search, Integer page, Integer size) {
		Page<Customer> result = customerManagement.search(search, page, size);
		return ResponseEntity.ok(new CustomerPage(result.map(CustomerController::toResponse).getContent(),
				result.getNumber(), result.getSize(), result.getTotalElements()));
	}

	@Override
	public ResponseEntity<de.photoffice.api.model.Customer> createCustomer(CustomerInput input) {
		Customer customer = customerManagement.create(toData(input));
		return ResponseEntity.created(URI.create("/api/studio/customers/" + customer.id())).body(toResponse(customer));
	}

	@Override
	public ResponseEntity<de.photoffice.api.model.Customer> getCustomer(UUID customerId) {
		return ResponseEntity.ok(toResponse(customerManagement.get(customerId)));
	}

	@Override
	public ResponseEntity<de.photoffice.api.model.Customer> updateCustomer(UUID customerId, CustomerInput input) {
		return ResponseEntity.ok(toResponse(customerManagement.update(customerId, toData(input))));
	}

	@Override
	public ResponseEntity<Void> deleteCustomer(UUID customerId) {
		customerManagement.delete(customerId);
		return ResponseEntity.noContent().build();
	}

	@ExceptionHandler
	ProblemDetail onNotFound(CustomerNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Der Kunde wurde nicht gefunden.");
	}

	@ExceptionHandler
	ProblemDetail onDuplicateEmail(DuplicateCustomerEmailException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler
	ProblemDetail onInvalidInput(IllegalArgumentException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	private static CustomerData toData(CustomerInput input) {
		return new CustomerData(input.getFirstName(), input.getLastName(), input.getEmail(), input.getPhone(),
				input.getStreet(), input.getPostalCode(), input.getCity(), input.getNotes());
	}

	private static de.photoffice.api.model.Customer toResponse(Customer customer) {
		var response = new de.photoffice.api.model.Customer(customer.firstName(), customer.lastName(),
				customer.email(), customer.id(), customer.createdAt().atOffset(ZoneOffset.UTC),
				customer.updatedAt().atOffset(ZoneOffset.UTC));
		response.setPhone(customer.phone());
		response.setStreet(customer.street());
		response.setPostalCode(customer.postalCode());
		response.setCity(customer.city());
		response.setNotes(customer.notes());
		return response;
	}

}
