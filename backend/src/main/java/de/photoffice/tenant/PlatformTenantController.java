package de.photoffice.tenant;

import de.photoffice.api.PlatformApi;
import de.photoffice.api.model.CreateTenantRequest;
import de.photoffice.api.model.TenantResponse;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
	public ResponseEntity<TenantResponse> suspendTenant(UUID tenantId) {
		return ResponseEntity.ok(toResponse(tenantManagement.suspend(TenantId.of(tenantId))));
	}

	@Override
	public ResponseEntity<TenantResponse> reactivateTenant(UUID tenantId) {
		return ResponseEntity.ok(toResponse(tenantManagement.reactivate(TenantId.of(tenantId))));
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
	ProblemDetail onUnknownTenant(TenantNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
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
				tenant.createdAt().atOffset(ZoneOffset.UTC));
	}

}
