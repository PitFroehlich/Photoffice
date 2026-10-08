package de.photoffice.pricing;

import java.util.UUID;

import static de.photoffice.pricing.Prices.requireValidCents;
import static de.photoffice.pricing.Prices.required;

/**
 * Editable fields of a download package: a number of images or the whole gallery at a package price, delivered
 * in one download variant (product of type {@code DOWNLOAD}).
 */
public record DownloadPackageData(String name, DownloadPackageKind kind, Integer imageCount, UUID downloadProductId,
		int priceCents, boolean active) {

	public DownloadPackageData {
		name = PriceListTexts.name(required(name, "Der Name des Pakets ist Pflicht."), "Name des Pakets");
		if (kind == null) {
			throw new IllegalArgumentException("Die Art des Pakets fehlt.");
		}
		if (downloadProductId == null) {
			throw new IllegalArgumentException("Die Download-Variante des Pakets fehlt.");
		}
		priceCents = requireValidCents(priceCents);
		switch (kind) {
			case IMAGE_COUNT -> {
				if (imageCount == null || imageCount < 2 || imageCount > 10_000) {
					throw new IllegalArgumentException("Ein Paket enthält zwischen 2 und 10.000 Bilder.");
				}
			}
			case WHOLE_GALLERY -> {
				if (imageCount != null) {
					throw new IllegalArgumentException("Ein Paket „ganze Galerie“ hat keine feste Bildanzahl.");
				}
			}
		}
	}

}
