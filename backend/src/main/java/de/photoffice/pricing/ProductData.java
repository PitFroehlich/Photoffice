package de.photoffice.pricing;

import static de.photoffice.pricing.Prices.optional;
import static de.photoffice.pricing.Prices.requireValidCents;
import static de.photoffice.pricing.Prices.required;

/**
 * Editable product fields, normalised and checked against the product type: a print needs paper type and
 * format, a download needs a variant name and optionally a maximum edge length (none = original); fields of
 * the other type must be empty.
 */
public record ProductData(ProductType type, String paperType, String printFormat, String downloadName,
		Integer maxEdgePx, int priceCents, boolean active) {

	static final int MIN_EDGE_PX = 200;

	static final int MAX_EDGE_PX = 20_000;

	public ProductData {
		if (type == null) {
			throw new IllegalArgumentException("Der Produkttyp fehlt.");
		}
		priceCents = requireValidCents(priceCents);
		switch (type) {
			case PRINT -> {
				paperType = PriceListTexts.paperType(required(paperType, "Für einen Abzug ist der Papiertyp Pflicht."));
				printFormat = PriceListTexts.printFormat(required(printFormat, "Für einen Abzug ist das Format Pflicht."));
				if (optional(downloadName) != null || maxEdgePx != null) {
					throw new IllegalArgumentException("Ein Abzug hat keinen Download-Namen und keine Pixelgröße.");
				}
				downloadName = null;
			}
			case DOWNLOAD -> {
				downloadName = PriceListTexts.name(
						required(downloadName, "Für einen Download ist der Name der Variante Pflicht."),
						"Name der Download-Variante");
				if (maxEdgePx != null && (maxEdgePx < MIN_EDGE_PX || maxEdgePx > MAX_EDGE_PX)) {
					throw new IllegalArgumentException(
							"Die Kantenlänge liegt zwischen 200 und 20.000 Pixeln – oder leer für das Original.");
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
