package de.photoffice.customer;

import java.util.Locale;

/**
 * Editable customer fields, normalised: trimmed, empty optional fields become {@code null}.
 */
public record CustomerData(String firstName, String lastName, String email, String phone, String street,
		String postalCode, String city, String notes) {

	public CustomerData {
		firstName = required(firstName, "firstName");
		lastName = required(lastName, "lastName");
		email = required(email, "email").toLowerCase(Locale.ROOT);
		phone = optional(phone);
		street = optional(street);
		postalCode = optional(postalCode);
		city = optional(city);
		notes = optional(notes);
	}

	private static String required(String value, String field) {
		String normalized = optional(value);
		if (normalized == null) {
			throw new IllegalArgumentException("'" + field + "' must not be blank");
		}
		return normalized;
	}

	private static String optional(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

}
