package de.photoffice.tenant;

import de.photoffice.api.PlatformApi;
import de.photoffice.api.model.CreateTenantRequest;
import de.photoffice.api.model.TenantResponse;
import de.photoffice.api.model.UpdateTenantRequest;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Studio administration for the platform operator (access restricted in {@code SecurityConfiguration}).
 */
@RestController
@RequestMapping("/api")
class PlatformTenantController implements PlatformApi {

	private final TenantManagement tenantManagement;

	PlatformTenantController(TenantManagement tenantManagement) {
		this.tenantManagement = tenantManagement;
	}

	@Override
	public ResponseEntity<TenantResponse> createTenant(CreateTenantRequest request) {
		var admin = new InitialStudioAdmin(request.getAdminEmail(), request.getAdminFirstName(),
				request.getAdminLastName());
		Tenant tenant = tenantManagement.register(request.getSlug(), request.getName(), admin);
		return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(tenant));
	}

	@Override
	public ResponseEntity<TenantResponse> getTenant(UUID tenantId) {
		return tenantManagement.findById(TenantId.of(tenantId))
			.map(tenant -> ResponseEntity.ok(toResponse(tenant)))
			.orElseThrow(() -> new TenantNotFoundException(TenantId.of(tenantId)));
	}

	@Override
	public ResponseEntity<TenantResponse> updateTenant(UUID tenantId, UpdateTenantRequest request) {
		TenantId id = TenantId.of(tenantId);
		Optional<InitialStudioAdmin> admin = initialAdmin(request);
		Tenant tenant = tenantManagement.update(id, request.getName(), admin);
		if (admin.isPresent() && !tenant.onboarded()) {
			// A failed onboarding is repeated right away with the corrected data (runs in the background)
			tenant = tenantManagement.retryOnboarding(id);
		}
		return ResponseEntity.ok(toResponse(tenant));
	}

	private static Optional<InitialStudioAdmin> initialAdmin(UpdateTenantRequest request) {
		if (request.getAdminEmail() == null) {
			if (request.getAdminFirstName() != null || request.getAdminLastName() != null) {
				throw new IllegalArgumentException(
						"Vor- und Nachname des ersten Studio-Admins nur zusammen mit der E-Mail-Adresse angeben.");
			}
			return Optional.empty();
		}
		return Optional.of(new InitialStudioAdmin(request.getAdminEmail(), request.getAdminFirstName(),
				request.getAdminLastName()));
	}

	@Override
	public ResponseEntity<TenantResponse> suspendTenant(UUID tenantId) {
		return ResponseEntity.ok(toResponse(tenantManagement.suspend(TenantId.of(tenantId))));
	}

	@Override
	public ResponseEntity<TenantResponse> reactivateTenant(UUID tenantId) {
		return ResponseEntity.ok(toResponse(tenantManagement.reactivate(TenantId.of(tenantId))));
	}

	@Override
	public ResponseEntity<TenantResponse> retryTenantOnboarding(UUID tenantId) {
		return ResponseEntity.accepted().body(toResponse(tenantManagement.retryOnboarding(TenantId.of(tenantId))));
	}

	@Override
	public ResponseEntity<List<TenantResponse>> listTenants() {
		return ResponseEntity.ok(tenantManagement.findAll().stream().map(PlatformTenantController::toResponse).toList());
	}

	@ExceptionHandler
	ProblemDetail onDuplicateSlug(DuplicateTenantSlugException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler
	ProblemDetail onOnboardingCompleted(OnboardingCompletedException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	/** Edited while the onboarding (or another operator) changed the studio. */
	@ExceptionHandler
	ProblemDetail onConcurrentChange(OptimisticLockingFailureException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"Das Studio wurde gerade an anderer Stelle geändert. Bitte laden Sie die Seite neu und versuchen Sie es erneut.");
	}

	@ExceptionHandler
	ProblemDetail onUnknownTenant(TenantNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	/** Path variable that is no UUID, e.g. {@code /api/platform/tenants/abc/suspend}. */
	@ExceptionHandler
	ProblemDetail onInvalidId(MethodArgumentTypeMismatchException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Ungültige Studio-ID „" + ex.getValue() + "“ – erwartet wird eine UUID.");
	}

	@ExceptionHandler
	ProblemDetail onInvalidInput(IllegalArgumentException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	private static TenantResponse toResponse(Tenant tenant) {
		return new TenantResponse(tenant.id().value(), tenant.slug(), tenant.name(),
				TenantResponse.StatusEnum.fromValue(tenant.status().name()),
				tenant.onboarded() ? TenantResponse.OnboardingStatusEnum.COMPLETED
						: TenantResponse.OnboardingStatusEnum.PENDING,
				tenant.createdAt().atOffset(ZoneOffset.UTC))
			.onboardingError(tenant.onboardingError().orElse(null))
			.onboardingFailedAt(tenant.onboardingFailedAt().map(at -> at.atOffset(ZoneOffset.UTC)).orElse(null))
			.adminEmail(tenant.initialAdmin().map(InitialStudioAdmin::email).orElse(null))
			.adminFirstName(tenant.initialAdmin().map(InitialStudioAdmin::firstName).orElse(null))
			.adminLastName(tenant.initialAdmin().map(InitialStudioAdmin::lastName).orElse(null));
	}

}
