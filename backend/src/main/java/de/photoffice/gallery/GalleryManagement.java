package de.photoffice.gallery;

import de.photoffice.customer.CustomerManagement;
import de.photoffice.pricing.PriceListManagement;
import de.photoffice.tenant.TenantContext;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Galleries of the current studio ({@link TenantContext}). Public API of the {@code gallery} module.
 */
@Service
@Transactional
public class GalleryManagement {

	/** Studios work in German time; "today" decides about expiry. Per-studio time zones can follow. */
	static final ZoneId STUDIO_ZONE = ZoneId.of("Europe/Berlin");

	private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("name"));

	private final GalleryRepository galleries;

	private final CustomerManagement customerManagement;

	private final PriceListManagement priceListManagement;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	GalleryManagement(GalleryRepository galleries, CustomerManagement customerManagement,
			PriceListManagement priceListManagement, ApplicationEventPublisher events, Optional<Clock> clock) {
		this.galleries = galleries;
		this.customerManagement = customerManagement;
		this.priceListManagement = priceListManagement;
		this.events = events;
		this.clock = clock.orElse(Clock.systemUTC());
	}

	@Transactional(readOnly = true)
	public Page<Gallery> search(String term, GalleryStatus status, UUID customerId, int page, int size) {
		Specification<Gallery> filter = Specification.unrestricted();
		if (term != null && !term.isBlank()) {
			filter = filter.and(GalleryRepository.nameContains(likePattern(term)));
		}
		if (status != null) {
			filter = filter.and(GalleryRepository.hasStatus(status));
		}
		if (customerId != null) {
			filter = filter.and(GalleryRepository.hasCustomer(customerId));
		}
		return galleries.findAll(filter, PageRequest.of(page, size, NEWEST_FIRST));
	}

	@Transactional(readOnly = true)
	public Gallery get(UUID id) {
		return galleries.findById(id).orElseThrow(GalleryNotFoundException::new);
	}

	public Gallery create(GalleryData data) {
		validate(data, null);
		return galleries.saveAndFlush(new Gallery(TenantContext.require(), data, now()));
	}

	public Gallery update(UUID id, GalleryData data) {
		Gallery gallery = get(id);
		validate(data, gallery);
		gallery.apply(data, now());
		return galleries.saveAndFlush(gallery);
	}

	public void delete(UUID id) {
		Gallery gallery = get(id);
		galleries.delete(gallery);
		events.publishEvent(new GalleryDeleted(gallery.tenantId(), gallery.id()));
	}

	/**
	 * Puts the gallery online. Customers can only use it while it is not expired and the studio sells something.
	 */
	public Gallery publish(UUID id) {
		Gallery gallery = get(id);
		if (gallery.status() == GalleryStatus.ONLINE) {
			throw new GalleryStateException("Die Galerie ist bereits online.");
		}
		if (gallery.isExpired(today())) {
			throw new GalleryStateException(
					"Die Galerie ist abgelaufen. Verlängern Sie das Ablaufdatum, bevor Sie sie veröffentlichen.");
		}
		if (!priceListManagement.hasActiveOffer()) {
			throw new GalleryStateException(
					"Ohne aktive Preise kann die Galerie nicht veröffentlicht werden. Legen Sie zuerst in der Preisliste Preise an.");
		}
		gallery.publish(now());
		Gallery saved = galleries.saveAndFlush(gallery);
		events.publishEvent(new GalleryPublished(saved.tenantId(), saved.id(), saved.customerIds()));
		return saved;
	}

	public Gallery unpublish(UUID id) {
		Gallery gallery = get(id);
		if (gallery.status() != GalleryStatus.ONLINE) {
			throw new GalleryStateException("Die Galerie ist nicht online.");
		}
		gallery.unpublish(now());
		return galleries.saveAndFlush(gallery);
	}

	public LocalDate today() {
		return LocalDate.now(clock.withZone(STUDIO_ZONE));
	}

	private void validate(GalleryData data, Gallery existing) {
		boolean expiryChanged = existing == null || !java.util.Objects.equals(existing.expiresOn(), data.expiresOn());
		if (expiryChanged && data.expiresOn() != null && data.expiresOn().isBefore(today())) {
			throw new IllegalArgumentException("Das Ablaufdatum darf nicht in der Vergangenheit liegen.");
		}
		// Customers of other studios are invisible (row-level security) and therefore count as missing
		if (customerManagement.findAllById(data.customerIds()).size() != data.customerIds().size()) {
			throw new IllegalArgumentException("Ein ausgewählter Kunde existiert nicht.");
		}
	}

	private Instant now() {
		return clock.instant().truncatedTo(ChronoUnit.MICROS);
	}

	/** Case-insensitive "contains" with LIKE wildcards of the user input escaped. */
	static String likePattern(String term) {
		String escaped = term.strip()
			.toLowerCase()
			.replace("\\", "\\\\")
			.replace("%", "\\%")
			.replace("_", "\\_");
		return "%" + escaped + "%";
	}

}
