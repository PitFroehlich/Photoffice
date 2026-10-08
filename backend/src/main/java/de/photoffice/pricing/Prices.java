package de.photoffice.pricing;

/**
 * Amounts are gross prices in cents of the price list currency – integers, never floating point (ADR 0008).
 * Validation messages are German because they are shown to studio users.
 */
final class Prices {

	static final int MAX_CENTS = 10_000_000;

	private Prices() {
	}

	static int requireValidCents(int cents) {
		if (cents < 0 || cents > MAX_CENTS) {
			throw new IllegalArgumentException("Der Preis muss zwischen 0,00 und 100.000,00 liegen.");
		}
		return cents;
	}

	static String required(String value, String message) {
		String normalized = optional(value);
		if (normalized == null) {
			throw new IllegalArgumentException(message);
		}
		return normalized;
	}

	static String optional(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

}
