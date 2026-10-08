package de.photoffice.gallery;

import de.photoffice.api.GalleriesApi;
import de.photoffice.api.model.GalleryCustomer;
import de.photoffice.api.model.GalleryInput;
import de.photoffice.api.model.GalleryPage;
import de.photoffice.customer.Customer;
import de.photoffice.customer.CustomerManagement;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class GalleryController implements GalleriesApi {

	private final GalleryManagement galleryManagement;

	private final CustomerManagement customerManagement;

	GalleryController(GalleryManagement galleryManagement, CustomerManagement customerManagement) {
		this.galleryManagement = galleryManagement;
		this.customerManagement = customerManagement;
	}

	@Override
	public ResponseEntity<GalleryPage> listGalleries(String search, de.photoffice.api.model.GalleryStatus status,
			UUID customerId, Integer page, Integer size) {
		Page<Gallery> result = galleryManagement.search(search,
				status == null ? null : GalleryStatus.valueOf(status.getValue()), customerId, page, size);
		List<de.photoffice.api.model.Gallery> items = toResponses(result.getContent());
		return ResponseEntity.ok(new GalleryPage(items, result.getNumber(), result.getSize(), result.getTotalElements()));
	}

	@Override
	public ResponseEntity<de.photoffice.api.model.Gallery> createGallery(GalleryInput input) {
		Gallery gallery = galleryManagement.create(toData(input));
		return ResponseEntity.created(URI.create("/api/studio/galleries/" + gallery.id())).body(toResponse(gallery));
	}

	@Override
	public ResponseEntity<de.photoffice.api.model.Gallery> getGallery(UUID galleryId) {
		return ResponseEntity.ok(toResponse(galleryManagement.get(galleryId)));
	}

	@Override
	public ResponseEntity<de.photoffice.api.model.Gallery> updateGallery(UUID galleryId, GalleryInput input) {
		return ResponseEntity.ok(toResponse(galleryManagement.update(galleryId, toData(input))));
	}

	@Override
	public ResponseEntity<Void> deleteGallery(UUID galleryId) {
		galleryManagement.delete(galleryId);
		return ResponseEntity.noContent().build();
	}

	@Override
	public ResponseEntity<de.photoffice.api.model.Gallery> publishGallery(UUID galleryId) {
		return ResponseEntity.ok(toResponse(galleryManagement.publish(galleryId)));
	}

	@Override
	public ResponseEntity<de.photoffice.api.model.Gallery> unpublishGallery(UUID galleryId) {
		return ResponseEntity.ok(toResponse(galleryManagement.unpublish(galleryId)));
	}

	@ExceptionHandler
	ProblemDetail onNotFound(GalleryNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler
	ProblemDetail onState(GalleryStateException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler
	ProblemDetail onInvalidInput(IllegalArgumentException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	private static GalleryData toData(GalleryInput input) {
		return new GalleryData(input.getName(), input.getDescription(), input.getExpiresOn(), input.getCustomerIds());
	}

	private de.photoffice.api.model.Gallery toResponse(Gallery gallery) {
		return toResponses(List.of(gallery)).getFirst();
	}

	/** Loads the customers of all galleries at once. */
	private List<de.photoffice.api.model.Gallery> toResponses(List<Gallery> galleries) {
		var customerIds = galleries.stream().flatMap(g -> g.customerIds().stream()).collect(Collectors.toSet());
		Map<UUID, Customer> customers = customerManagement.findAllById(customerIds)
			.stream()
			.collect(Collectors.toMap(Customer::id, Function.identity()));
		LocalDate today = galleryManagement.today();
		return galleries.stream().map(gallery -> toResponse(gallery, customers, today)).toList();
	}

	private static de.photoffice.api.model.Gallery toResponse(Gallery gallery, Map<UUID, Customer> customers,
			LocalDate today) {
		List<GalleryCustomer> assigned = gallery.customerIds()
			.stream()
			.map(customers::get)
			.filter(java.util.Objects::nonNull)
			.sorted(Comparator.comparing(Customer::lastName).thenComparing(Customer::firstName))
			.map(c -> new GalleryCustomer(c.id(), c.firstName(), c.lastName(), c.email()))
			.toList();
		var response = new de.photoffice.api.model.Gallery(gallery.id(), gallery.name(),
				de.photoffice.api.model.GalleryStatus.fromValue(gallery.status().name()), gallery.isExpired(today),
				assigned, utc(gallery.createdAt()), utc(gallery.updatedAt()));
		response.setDescription(gallery.description());
		response.setExpiresOn(gallery.expiresOn());
		response.setPublishedAt(gallery.publishedAt() == null ? null : utc(gallery.publishedAt()));
		return response;
	}

	private static OffsetDateTime utc(Instant instant) {
		return instant.atOffset(ZoneOffset.UTC);
	}

}
