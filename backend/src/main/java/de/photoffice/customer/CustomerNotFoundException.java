package de.photoffice.customer;

import java.util.UUID;

public class CustomerNotFoundException extends RuntimeException {

	public CustomerNotFoundException(UUID id) {
		super("Customer " + id + " not found");
	}

}
