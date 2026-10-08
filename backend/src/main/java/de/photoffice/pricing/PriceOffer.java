package de.photoffice.pricing;

import java.math.BigDecimal;
import java.util.List;

/**
 * What customers of a studio can buy: only active entries, i.e. only valid combinations of paper type and
 * format. Basis for the shop and orders (#14); prices are gross prices in cents of {@code currency}.
 */
public record PriceOffer(String currency, BigDecimal vatRatePercent, List<Product> prints, List<Product> downloads,
		List<DownloadPackage> downloadPackages, List<ShippingMethod> shippingMethods) {

	public PriceOffer {
		prints = List.copyOf(prints);
		downloads = List.copyOf(downloads);
		downloadPackages = List.copyOf(downloadPackages);
		shippingMethods = List.copyOf(shippingMethods);
	}

}
