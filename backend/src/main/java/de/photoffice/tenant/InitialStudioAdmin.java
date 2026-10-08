package de.photoffice.tenant;

import java.util.Locale;
import java.util.Objects;

/**
 * First administrator of a newly registered studio. Receives an invitation to set a password.
 *
 * @param email login and contact address (stored lowercase)
 * @param firstName optional
 * @param lastName optional
 */
public record InitialStudioAdmin(String email, String firstName, String lastName) {

	public InitialStudioAdmin {
		Objects.requireNonNull(email, "email");
		email = email.strip().toLowerCase(Locale.ROOT);
		if (email.isEmpty() || !email.contains("@")) {
			throw new IllegalArgumentException("Invalid admin e-mail: " + email);
		}
		firstName = blankToNull(firstName);
		lastName = blankToNull(lastName);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

}
