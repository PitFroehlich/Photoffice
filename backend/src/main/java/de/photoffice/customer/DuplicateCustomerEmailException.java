package de.photoffice.customer;

public class DuplicateCustomerEmailException extends RuntimeException {

	public DuplicateCustomerEmailException(String email) {
		super("Ein Kunde mit der E-Mail-Adresse " + email + " existiert bereits.");
	}

}
