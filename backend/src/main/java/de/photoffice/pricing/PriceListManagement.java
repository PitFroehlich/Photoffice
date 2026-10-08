package de.photoffice.pricing;

import de.photoffice.tenant.TenantContext;
import de.photoffice.tenant.TenantId;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Price list of the current studio ({@link TenantContext}): settings, products with single prices, download
 * packages and shipping methods. Public API of the {@code pricing} module.
 * <p>
 * Who may change the price list is decided in {@code SecurityConfiguration} (studio administrators only).
 */
@Service
@Transactional
public class PriceListManagement {

	private static final UUID NO_ID = new UUID(0, 0);

	private final PriceListSettingsRepository settings;

	private final ProductRepository products;

	private final DownloadPackageRepository downloadPackages;

	private final ShippingMethodRepository shippingMethods;

	private final Clock clock;

	PriceListManagement(PriceListSettingsRepository settings, ProductRepository products,
			DownloadPackageRepository downloadPackages, ShippingMethodRepository shippingMethods,
			Optional<Clock> clock) {
		this.settings = settings;
		this.products = products;
		this.downloadPackages = downloadPackages;
		this.shippingMethods = shippingMethods;
		this.clock = clock.orElse(Clock.systemUTC());
	}

	/** Complete price list including inactive entries (for the studio). */
	@Transactional(readOnly = true)
	public PriceList priceList() {
		return new PriceList(currentSettings(), products.findAllOrdered(), downloadPackages.findAllOrdered(),
				shippingMethods.findAllOrdered());
	}

	/** What customers can buy: active entries only (for the shop and orders, #14). */
	@Transactional(readOnly = true)
	public PriceOffer offer() {
		PriceListSettings current = currentSettings();
		var activeProducts = products.findAllOrdered().stream().filter(Product::active).toList();
		var activeDownloadIds = activeProducts.stream()
			.filter(p -> p.type() == ProductType.DOWNLOAD)
			.map(Product::id)
			.collect(java.util.stream.Collectors.toSet());
		return new PriceOffer(current.currency(), current.vatRatePercent(),
				activeProducts.stream().filter(p -> p.type() == ProductType.PRINT).toList(),
				activeProducts.stream().filter(p -> p.type() == ProductType.DOWNLOAD).toList(),
				// A package is only offered while its download variant is active, too
				downloadPackages.findAllOrdered()
					.stream()
					.filter(DownloadPackage::active)
					.filter(d -> activeDownloadIds.contains(d.downloadProductId()))
					.toList(),
				shippingMethods.findAllOrdered().stream().filter(ShippingMethod::active).toList());
	}

	/** Whether customers could buy anything at all – a gallery is only published with an active offer (#8). */
	@Transactional(readOnly = true)
	public boolean hasActiveOffer() {
		PriceOffer offer = offer();
		return !offer.prints().isEmpty() || !offer.downloads().isEmpty() || !offer.downloadPackages().isEmpty();
	}

	// --- settings ---

	public PriceListSettings changeVatRate(BigDecimal vatRatePercent) {
		TenantId tenant = TenantContext.require();
		PriceListSettings current = settings.findById(tenant.value())
			.orElseGet(() -> new PriceListSettings(tenant, now()));
		current.changeVatRate(vatRatePercent, now());
		return settings.saveAndFlush(current);
	}

	private PriceListSettings currentSettings() {
		TenantId tenant = TenantContext.require();
		return settings.findById(tenant.value()).orElseGet(() -> new PriceListSettings(tenant, now()));
	}

	// --- products ---

	@Transactional(readOnly = true)
	public Product product(UUID id) {
		return products.findById(id).orElseThrow(PriceListEntryNotFoundException::product);
	}

	public Product createProduct(ProductData data) {
		TenantId tenant = TenantContext.require();
		ensureProductIsNew(data, NO_ID);
		return saveChecked(products, new Product(tenant, data, now()), () -> DuplicatePriceListEntryException.of(data));
	}

	public Product updateProduct(UUID id, ProductData data) {
		Product product = product(id);
		ensureProductIsNew(data, id);
		product.apply(data, now());
		return saveChecked(products, product, () -> DuplicatePriceListEntryException.of(data));
	}

	/**
	 * A download variant that packages are delivered in cannot be deleted (it can be deactivated instead).
	 */
	public void deleteProduct(UUID id) {
		Product product = product(id);
		List<DownloadPackage> usedBy = downloadPackages.findByDownloadProductIdOrderByName(id);
		if (!usedBy.isEmpty()) {
			throw new PriceListEntryInUseException(
					"Die Download-Variante „%s“ wird vom Paket „%s“ verwendet und kann nicht gelöscht werden. Deaktivieren Sie sie stattdessen."
						.formatted(product.downloadName(), usedBy.getFirst().name()));
		}
		products.delete(product);
	}

	private void ensureProductIsNew(ProductData data, UUID excludedId) {
		boolean exists = switch (data.type()) {
			case PRINT -> products.existsOtherPrint(data.paperType(), data.printFormat(), excludedId);
			case DOWNLOAD -> products.existsOtherDownload(data.downloadName(), excludedId);
		};
		if (exists) {
			throw DuplicatePriceListEntryException.of(data);
		}
	}

	// --- download packages ---

	@Transactional(readOnly = true)
	public DownloadPackage downloadPackage(UUID id) {
		return downloadPackages.findById(id).orElseThrow(PriceListEntryNotFoundException::downloadPackage);
	}

	public DownloadPackage createDownloadPackage(DownloadPackageData data) {
		TenantId tenant = TenantContext.require();
		ensureDownloadVariantExists(data);
		ensureUnique(name -> downloadPackages.existsOtherWithName(name, NO_ID), data.name(),
				() -> DuplicatePriceListEntryException.of(data));
		return saveChecked(downloadPackages, new DownloadPackage(tenant, data, now()),
				() -> DuplicatePriceListEntryException.of(data));
	}

	public DownloadPackage updateDownloadPackage(UUID id, DownloadPackageData data) {
		DownloadPackage downloadPackage = downloadPackage(id);
		ensureDownloadVariantExists(data);
		ensureUnique(name -> downloadPackages.existsOtherWithName(name, id), data.name(),
				() -> DuplicatePriceListEntryException.of(data));
		downloadPackage.apply(data, now());
		return saveChecked(downloadPackages, downloadPackage, () -> DuplicatePriceListEntryException.of(data));
	}

	/** Row-level security makes variants of other studios invisible, so they count as missing. */
	private void ensureDownloadVariantExists(DownloadPackageData data) {
		products.findById(data.downloadProductId())
			.filter(product -> product.type() == ProductType.DOWNLOAD)
			.orElseThrow(() -> new IllegalArgumentException("Die gewählte Download-Variante gibt es nicht."));
	}

	public void deleteDownloadPackage(UUID id) {
		downloadPackages.delete(downloadPackage(id));
	}

	// --- shipping methods ---

	@Transactional(readOnly = true)
	public ShippingMethod shippingMethod(UUID id) {
		return shippingMethods.findById(id).orElseThrow(PriceListEntryNotFoundException::shippingMethod);
	}

	public ShippingMethod createShippingMethod(ShippingMethodData data) {
		TenantId tenant = TenantContext.require();
		ensureUnique(name -> shippingMethods.existsOtherWithName(name, NO_ID), data.name(),
				() -> DuplicatePriceListEntryException.of(data));
		return saveChecked(shippingMethods, new ShippingMethod(tenant, data, now()),
				() -> DuplicatePriceListEntryException.of(data));
	}

	public ShippingMethod updateShippingMethod(UUID id, ShippingMethodData data) {
		ShippingMethod shippingMethod = shippingMethod(id);
		ensureUnique(name -> shippingMethods.existsOtherWithName(name, id), data.name(),
				() -> DuplicatePriceListEntryException.of(data));
		shippingMethod.apply(data, now());
		return saveChecked(shippingMethods, shippingMethod, () -> DuplicatePriceListEntryException.of(data));
	}

	public void deleteShippingMethod(UUID id) {
		shippingMethods.delete(shippingMethod(id));
	}

	// --- helpers ---

	private static void ensureUnique(Predicate<String> nameTaken, String name,
			Supplier<DuplicatePriceListEntryException> duplicate) {
		if (nameTaken.test(name)) {
			throw duplicate.get();
		}
	}

	/** The unique indexes decide in case of concurrent requests. */
	private static <T> T saveChecked(JpaRepository<T, UUID> repository, T entity,
			Supplier<DuplicatePriceListEntryException> duplicate) {
		try {
			return repository.saveAndFlush(entity);
		}
		catch (DataIntegrityViolationException ex) {
			throw duplicate.get();
		}
	}

	private Instant now() {
		return clock.instant().truncatedTo(ChronoUnit.MICROS);
	}

}
