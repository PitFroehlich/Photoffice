package de.photoffice.pricing;

import java.util.List;

/**
 * The complete price list of a studio as maintained by the studio, including inactive entries.
 */
public record PriceList(PriceListSettings settings, List<Product> products, List<DownloadPackage> downloadPackages,
		List<ShippingMethod> shippingMethods) {

	public PriceList {
		products = List.copyOf(products);
		downloadPackages = List.copyOf(downloadPackages);
		shippingMethods = List.copyOf(shippingMethods);
	}

}
