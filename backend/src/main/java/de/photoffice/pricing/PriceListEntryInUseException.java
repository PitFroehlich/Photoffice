package de.photoffice.pricing;

/**
 * An entry cannot be deleted because other entries refer to it. German message, shown to studio users.
 */
public class PriceListEntryInUseException extends RuntimeException {

	PriceListEntryInUseException(String message) {
		super(message);
	}

}
