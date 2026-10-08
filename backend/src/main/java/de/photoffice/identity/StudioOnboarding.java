package de.photoffice.identity;

import de.photoffice.tenant.InitialStudioAdmin;
import de.photoffice.tenant.Tenant;
import de.photoffice.tenant.TenantManagement;
import de.photoffice.tenant.TenantRegistered;
import de.photoffice.tenant.TenantStatus;
import de.photoffice.tenant.TenantStatusChanged;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Sets up a newly registered studio in Keycloak: organization (alias = tenant slug), first studio admin with role
 * {@code studio-admin} as member, and an invitation e-mail to set the password.
 * <p>
 * Runs asynchronously after the registration committed. Every step checks what already exists, so the listener can
 * be repeated after a failure (Spring Modulith keeps the event publication incomplete and it is resubmitted, see
 * {@code EventResubmission}). The reason of a failure is recorded at the studio for the platform operator and cleared
 * once onboarding succeeds. Suspending a studio disables its organization.
 */
class StudioOnboarding {

	static final String UPDATE_PASSWORD = "UPDATE_PASSWORD";

	static final String VERIFY_EMAIL = "VERIFY_EMAIL";

	private static final String STUDIO_ADMIN_ROLE = "studio-admin";

	private static final Logger log = LoggerFactory.getLogger(StudioOnboarding.class);

	private final KeycloakAdminClient keycloak;

	private final TenantManagement tenantManagement;

	StudioOnboarding(KeycloakAdminClient keycloak, TenantManagement tenantManagement) {
		this.keycloak = keycloak;
		this.tenantManagement = tenantManagement;
	}

	/**
	 * A failure is recorded at the studio (visible to the platform operator) and rethrown, so that Spring Modulith
	 * marks the event publication as failed and it is delivered again later.
	 */
	@ApplicationModuleListener
	void on(TenantRegistered event) {
		try {
			onboard(event);
		}
		catch (RuntimeException ex) {
			String reason = failureReason(ex);
			log.warn("Onboarding of studio '{}' failed, will be retried: {}", event.slug(), reason, ex);
			try {
				tenantManagement.recordOnboardingFailure(event.tenantId(), reason);
			}
			catch (RuntimeException recordingFailed) {
				ex.addSuppressed(recordingFailed);
			}
			throw ex;
		}
	}

	private void onboard(TenantRegistered event) {
		Tenant tenant = tenantManagement.findById(event.tenantId())
			.orElseThrow(() -> new IllegalStateException("Registered tenant not found: " + event.tenantId()));
		if (tenant.onboarded()) {
			return;
		}
		boolean active = tenant.status() == TenantStatus.ACTIVE;

		String organizationId = keycloak.findOrganization(tenant.slug())
			.map(KeycloakAdminClient.Organization::id)
			.orElseGet(() -> keycloak.createOrganization(tenant.slug(), tenant.name(), active));
		// The studio may have been suspended in the meantime
		keycloak.setOrganizationEnabled(organizationId, active);

		KeycloakAdminClient.User admin = studioAdmin(event.admin(), tenant.slug());
		keycloak.assignRealmRole(admin.id(), STUDIO_ADMIN_ROLE);
		keycloak.addMember(organizationId, admin.id());
		if (admin.hasRequiredAction(UPDATE_PASSWORD)) {
			keycloak.sendActionsEmail(admin.id(), List.of(UPDATE_PASSWORD, VERIFY_EMAIL));
		}

		tenantManagement.markOnboarded(tenant.id());
		log.info("Studio '{}' onboarded: Keycloak organization {}, studio admin {} invited", tenant.slug(),
				organizationId, admin.email());
	}

	@ApplicationModuleListener
	void on(TenantStatusChanged event) {
		// No organization yet: onboarding creates it with the current status
		keycloak.findOrganization(event.slug())
			.ifPresent(organization -> keycloak.setOrganizationEnabled(organization.id(),
					event.status() == TenantStatus.ACTIVE));
	}

	/**
	 * Finds or creates the admin user. A studio user must belong to exactly one studio, so an existing user from
	 * another studio is rejected.
	 */
	private KeycloakAdminClient.User studioAdmin(InitialStudioAdmin admin, String slug) {
		KeycloakAdminClient.User user = keycloak.findUserByEmail(admin.email())
			.orElseGet(() -> keycloak.createUser(admin.email(), admin.firstName(), admin.lastName(),
					List.of(UPDATE_PASSWORD, VERIFY_EMAIL)));
		List<String> otherStudios = keycloak.organizationsOf(user.id()).stream().filter(s -> !s.equals(slug)).toList();
		if (!otherStudios.isEmpty()) {
			String studios = otherStudios.stream().map(this::studioLabel).collect(Collectors.joining(", "));
			throw new OnboardingFailedException("Die E-Mail-Adresse " + admin.email() + " gehört bereits zu "
					+ (otherStudios.size() == 1 ? "Studio " : "den Studios ") + studios
					+ ". Ein Benutzer kann nur zu einem Studio gehören.");
		}
		return user;
	}

	/** "Studio A (studio-a)", or only the slug if the organization has no studio (anymore). */
	private String studioLabel(String slug) {
		return tenantManagement.findBySlug(slug)
			.map(tenant -> "„" + tenant.name() + "“ (" + slug + ")")
			.orElse("„" + slug + "“");
	}

	/** German reason for the platform operator. */
	static String failureReason(RuntimeException ex) {
		if (ex instanceof OnboardingFailedException) {
			return ex.getMessage();
		}
		if (ex instanceof ResourceAccessException) {
			return "Keycloak ist nicht erreichbar.";
		}
		if (ex instanceof RestClientResponseException response) {
			return "Keycloak hat eine Anfrage abgelehnt (HTTP " + response.getStatusCode().value() + ").";
		}
		return "Unerwarteter Fehler: " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
	}

	/** Onboarding cannot complete until somebody resolves the cause; the message is shown to the platform operator. */
	static class OnboardingFailedException extends RuntimeException {

		OnboardingFailedException(String message) {
			super(message);
		}

	}

}
