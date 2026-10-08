package de.photoffice.studioprofile;

import java.util.regex.Pattern;

/**
 * Country of the studio address. Determines the postal code format.
 */
public enum Country {

	DE("^[0-9]{5}$", "PLZ: 5 Ziffern für Deutschland"),

	AT("^[0-9]{4}$", "PLZ: 4 Ziffern für Österreich"),

	CH("^[0-9]{4}$", "PLZ: 4 Ziffern für die Schweiz");

	private final Pattern postalCode;

	private final String postalCodeMessage;

	Country(String postalCode, String postalCodeMessage) {
		this.postalCode = Pattern.compile(postalCode);
		this.postalCodeMessage = postalCodeMessage;
	}

	Pattern postalCode() {
		return postalCode;
	}

	String postalCodeMessage() {
		return postalCodeMessage;
	}

}
