package de.photoffice.pricing;

import static de.photoffice.pricing.Prices.requireValidCents;
import static de.photoffice.pricing.Prices.required;

/**
 * Editable fields of a shipping method (only relevant for orders containing prints).
 */
public record ShippingMethodData(String name, int priceCents, boolean active) {

	public ShippingMethodData {
		name = required(name, "Der Name der Versandart ist Pflicht.");
		priceCents = requireValidCents(priceCents);
	}

}
