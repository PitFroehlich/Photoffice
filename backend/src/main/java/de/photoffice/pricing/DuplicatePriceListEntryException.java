package de.photoffice.pricing;

/**
 * An entry with the same identifying attributes already exists in the studio's price list. The message is
 * German because it is shown to studio users.
 */
public class DuplicatePriceListEntryException extends RuntimeException {

	DuplicatePriceListEntryException(String message) {
		super(message);
	}

	static DuplicatePriceListEntryException of(ProductData product) {
		return switch (product.type()) {
			case PRINT -> new DuplicatePriceListEntryException("Den Abzug „%s, %s“ gibt es bereits."
				.formatted(product.paperType(), product.printFormat()));
			case DOWNLOAD -> new DuplicatePriceListEntryException("Den Download in %s gibt es bereits."
				.formatted(product.resolution() == DownloadResolution.WEB ? "Web-Auflösung" : "voller Auflösung"));
		};
	}

	static DuplicatePriceListEntryException of(DownloadPackageData downloadPackage) {
		return new DuplicatePriceListEntryException(
				"Ein Paket mit dem Namen „%s“ gibt es bereits.".formatted(downloadPackage.name()));
	}

	static DuplicatePriceListEntryException of(ShippingMethodData shippingMethod) {
		return new DuplicatePriceListEntryException(
				"Eine Versandart mit dem Namen „%s“ gibt es bereits.".formatted(shippingMethod.name()));
	}

}
