package de.photoffice.pricing;

import de.photoffice.api.PriceListApi;
import de.photoffice.api.model.DownloadPackageInput;
import de.photoffice.api.model.PriceListSettingsInput;
import de.photoffice.api.model.ProductInput;
import de.photoffice.api.model.ShippingMethodInput;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class PriceListController implements PriceListApi {

	private static final String BASE = "/api/studio/price-list";

	private final PriceListManagement priceList;

	PriceListController(PriceListManagement priceList) {
		this.priceList = priceList;
	}

	@Override
	public ResponseEntity<de.photoffice.api.model.PriceList> getPriceList() {
		PriceList list = priceList.priceList();
		return ResponseEntity.ok(new de.photoffice.api.model.PriceList(toResponse(list.settings()),
				list.products().stream().map(PriceListController::toResponse).toList(),
				list.downloadPackages().stream().map(PriceListController::toResponse).toList(),
				list.shippingMethods().stream().map(PriceListController::toResponse).toList()));
	}

	@Override
	public ResponseEntity<de.photoffice.api.model.PriceListSettings> updatePriceListSettings(
			PriceListSettingsInput input) {
		return ResponseEntity.ok(toResponse(priceList.changeVatRate(input.getVatRatePercent())));
	}

	// --- products ---

	@Override
	public ResponseEntity<de.photoffice.api.model.Product> createProduct(ProductInput input) {
		Product product = priceList.createProduct(toData(input));
		return ResponseEntity.created(URI.create(BASE + "/products/" + product.id())).body(toResponse(product));
	}

	@Override
	public ResponseEntity<de.photoffice.api.model.Product> getProduct(UUID productId) {
		return ResponseEntity.ok(toResponse(priceList.product(productId)));
	}

	@Override
	public ResponseEntity<de.photoffice.api.model.Product> updateProduct(UUID productId, ProductInput input) {
		return ResponseEntity.ok(toResponse(priceList.updateProduct(productId, toData(input))));
	}

	@Override
	public ResponseEntity<Void> deleteProduct(UUID productId) {
		priceList.deleteProduct(productId);
		return ResponseEntity.noContent().build();
	}

	// --- download packages ---

	@Override
	public ResponseEntity<de.photoffice.api.model.DownloadPackage> createDownloadPackage(DownloadPackageInput input) {
		DownloadPackage created = priceList.createDownloadPackage(toData(input));
		return ResponseEntity.created(URI.create(BASE + "/download-packages/" + created.id()))
			.body(toResponse(created));
	}

	@Override
	public ResponseEntity<de.photoffice.api.model.DownloadPackage> getDownloadPackage(UUID packageId) {
		return ResponseEntity.ok(toResponse(priceList.downloadPackage(packageId)));
	}

	@Override
	public ResponseEntity<de.photoffice.api.model.DownloadPackage> updateDownloadPackage(UUID packageId,
			DownloadPackageInput input) {
		return ResponseEntity.ok(toResponse(priceList.updateDownloadPackage(packageId, toData(input))));
	}

	@Override
	public ResponseEntity<Void> deleteDownloadPackage(UUID packageId) {
		priceList.deleteDownloadPackage(packageId);
		return ResponseEntity.noContent().build();
	}

	// --- shipping methods ---

	@Override
	public ResponseEntity<de.photoffice.api.model.ShippingMethod> createShippingMethod(ShippingMethodInput input) {
		ShippingMethod created = priceList.createShippingMethod(toData(input));
		return ResponseEntity.created(URI.create(BASE + "/shipping-methods/" + created.id()))
			.body(toResponse(created));
	}

	@Override
	public ResponseEntity<de.photoffice.api.model.ShippingMethod> getShippingMethod(UUID shippingMethodId) {
		return ResponseEntity.ok(toResponse(priceList.shippingMethod(shippingMethodId)));
	}

	@Override
	public ResponseEntity<de.photoffice.api.model.ShippingMethod> updateShippingMethod(UUID shippingMethodId,
			ShippingMethodInput input) {
		return ResponseEntity.ok(toResponse(priceList.updateShippingMethod(shippingMethodId, toData(input))));
	}

	@Override
	public ResponseEntity<Void> deleteShippingMethod(UUID shippingMethodId) {
		priceList.deleteShippingMethod(shippingMethodId);
		return ResponseEntity.noContent().build();
	}

	// --- errors ---

	@ExceptionHandler
	ProblemDetail onNotFound(PriceListEntryNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler
	ProblemDetail onDuplicate(DuplicatePriceListEntryException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler
	ProblemDetail onInUse(PriceListEntryInUseException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler
	ProblemDetail onInvalidInput(IllegalArgumentException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	// --- mapping ---

	private static ProductData toData(ProductInput input) {
		return new ProductData(ProductType.valueOf(input.getType().getValue()), input.getPaperType(),
				input.getPrintFormat(), input.getDownloadName(), input.getMaxEdgePx(), input.getPriceCents(),
				isActive(input.getActive()));
	}

	private static DownloadPackageData toData(DownloadPackageInput input) {
		return new DownloadPackageData(input.getName(), DownloadPackageKind.valueOf(input.getKind().getValue()),
				input.getImageCount(), input.getDownloadProductId(), input.getPriceCents(), isActive(input.getActive()));
	}

	private static ShippingMethodData toData(ShippingMethodInput input) {
		return new ShippingMethodData(input.getName(), input.getPriceCents(), isActive(input.getActive()));
	}

	/** Missing {@code active} means active (OpenAPI default). */
	private static boolean isActive(Boolean active) {
		return active == null || active;
	}

	private static de.photoffice.api.model.PriceListSettings toResponse(PriceListSettings settings) {
		return new de.photoffice.api.model.PriceListSettings(
				de.photoffice.api.model.PriceListSettings.CurrencyEnum.fromValue(settings.currency()),
				settings.vatRatePercent());
	}

	private static de.photoffice.api.model.Product toResponse(Product product) {
		var response = new de.photoffice.api.model.Product(
				de.photoffice.api.model.ProductType.fromValue(product.type().name()), product.priceCents(),
				product.active(), product.id(), utc(product.createdAt()), utc(product.updatedAt()));
		response.setPaperType(product.paperType());
		response.setPrintFormat(product.printFormat());
		response.setDownloadName(product.downloadName());
		response.setMaxEdgePx(product.maxEdgePx());
		return response;
	}

	private static de.photoffice.api.model.DownloadPackage toResponse(DownloadPackage downloadPackage) {
		var response = new de.photoffice.api.model.DownloadPackage(downloadPackage.name(),
				de.photoffice.api.model.DownloadPackageKind.fromValue(downloadPackage.kind().name()),
				downloadPackage.downloadProductId(),
				downloadPackage.priceCents(), downloadPackage.active(), downloadPackage.id(),
				utc(downloadPackage.createdAt()), utc(downloadPackage.updatedAt()));
		response.setImageCount(downloadPackage.imageCount());
		return response;
	}

	private static de.photoffice.api.model.ShippingMethod toResponse(ShippingMethod shippingMethod) {
		return new de.photoffice.api.model.ShippingMethod(shippingMethod.name(), shippingMethod.priceCents(),
				shippingMethod.active(), shippingMethod.id(), utc(shippingMethod.createdAt()),
				utc(shippingMethod.updatedAt()));
	}

	private static OffsetDateTime utc(Instant instant) {
		return instant.atOffset(ZoneOffset.UTC);
	}

}
