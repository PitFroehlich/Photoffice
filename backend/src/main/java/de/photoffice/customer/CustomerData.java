package de.photoffice.customer;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Editable customer fields, normalised (trimmed, empty optional fields become {@code null}) and validated.
 * The same rules are applied in the Angular form ({@code customer-validators.ts}); messages are German because
 * they are shown to studio users.
 */
public record CustomerData(String firstName, String lastName, String email, String phone, String street,
		String postalCode, String city, String notes) {

	static final Pattern NAME = Pattern.compile("^\\p{L}[\\p{L} .'-]*$");

	static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@.]{2,}$");

	static final Pattern PHONE = Pattern.compile("^\\+?[0-9][0-9 ()/-]{3,28}[0-9]$");

	static final Pattern POSTAL_CODE = Pattern.compile("^[0-9]{4,5}$");

	static final Pattern CITY = Pattern.compile("^\\p{L}[\\p{L} .'()/-]*$");

	static final Pattern STREET = Pattern.compile("^(?=.*\\p{L})[\\p{L}0-9 .,'/-]+$");

	public CustomerData {
		firstName = check(required(firstName, "Vorname"), NAME,
				"Vorname: nur Buchstaben, Leerzeichen, Bindestrich, Apostroph und Punkt");
		lastName = check(required(lastName, "Nachname"), NAME,
				"Nachname: nur Buchstaben, Leerzeichen, Bindestrich, Apostroph und Punkt");
		email = check(required(email, "E-Mail"), EMAIL, "E-Mail: bitte eine gültige E-Mail-Adresse angeben")
			.toLowerCase(Locale.ROOT);
		phone = check(optional(phone), PHONE, "Telefon: nur Ziffern, Leerzeichen und + - / ( ), mindestens 5 Zeichen");
		street = check(optional(street), STREET, "Straße: Buchstaben, Ziffern und . , ' / -");
		postalCode = check(optional(postalCode), POSTAL_CODE, "PLZ: 4 oder 5 Ziffern");
		city = check(optional(city), CITY, "Ort: nur Buchstaben, Leerzeichen und - . ' ( ) /");
		notes = optional(notes);
	}

	private static String required(String value, String field) {
		String normalized = optional(value);
		if (normalized == null) {
			throw new IllegalArgumentException(field + " ist ein Pflichtfeld");
		}
		return normalized;
	}

	private static String optional(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

	private static String check(String value, Pattern pattern, String message) {
		if (value != null && !pattern.matcher(value).matches()) {
			throw new IllegalArgumentException(message);
		}
		return value;
	}

}
