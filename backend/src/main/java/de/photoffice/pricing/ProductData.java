package de.photoffice.pricing;

import static de.photoffice.pricing.Prices.optional;
import static de.photoffice.pricing.Prices.requireValidCents;
import static de.photoffice.pricing.Prices.required;

/**
 * Editable product fields, normalised and checked against the product type: a print needs paper type and
 * format, a download needs a resolution; fields of the other type must be empty.
 */
public record ProductData(ProductType type, String paperType, String printFormat, DownloadResolution resolution,
		int priceCents, boolean active) {

	public ProductData {
		if (type == null) {
			throw new IllegalArgumentException("Der Produkttyp fehlt.");
		}
		priceCents = requireValidCents(priceCents);
		switch (type) {
			case PRINT -> {
				paperType = PriceListTexts.paperType(required(paperType, "Für einen Abzug ist der Papiertyp Pflicht."));
				printFormat = PriceListTexts.printFormat(required(printFormat, "Für einen Abzug ist das Format Pflicht."));
				if (resolution != null) {
					throw new IllegalArgumentException("Ein Abzug hat keine Download-Auflösung.");
				}
			}
			case DOWNLOAD -> {
				if (resolution == null) {
					throw new IllegalArgumentException("Für einen Download ist die Auflösung Pflicht.");
				}
				if (optional(paperType) != null || optional(printFormat) != null) {
					throw new IllegalArgumentException("Ein Download hat keinen Papiertyp und kein Format.");
				}
				paperType = null;
				printFormat = null;
			}
		}
	}

}
