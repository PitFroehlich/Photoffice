package de.photoffice.pricing;

/**
 * A product, download package or shipping method does not exist in the current studio. The message is German
 * because it is shown to studio users.
 */
public class PriceListEntryNotFoundException extends RuntimeException {

	private PriceListEntryNotFoundException(String message) {
		super(message);
	}

	static PriceListEntryNotFoundException product() {
		return new PriceListEntryNotFoundException("Das Produkt wurde nicht gefunden.");
	}

	static PriceListEntryNotFoundException downloadPackage() {
		return new PriceListEntryNotFoundException("Das Download-Paket wurde nicht gefunden.");
	}

	static PriceListEntryNotFoundException shippingMethod() {
		return new PriceListEntryNotFoundException("Die Versandart wurde nicht gefunden.");
	}

}
