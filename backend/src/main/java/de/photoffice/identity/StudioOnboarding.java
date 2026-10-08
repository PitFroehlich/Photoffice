package de.photoffice.identity;

import de.photoffice.tenant.InitialStudioAdmin;
import de.photoffice.tenant.Tenant;
import de.photoffice.tenant.TenantManagement;
import de.photoffice.tenant.TenantRegistered;
import de.photoffice.tenant.TenantStatus;
import de.photoffice.tenant.TenantStatusChanged;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;

/**
 * Sets up a newly registered studio in Keycloak: organization (alias = tenant slug), first studio admin with role
 * {@code studio-admin} as member, and an invitation e-mail to set the password.
 * <p>
 * Runs asynchronously after the registration committed. Every step checks what already exists, so the listener can
 * be repeated after a failure (Spring Modulith keeps the event publication incomplete and it is resubmitted, see
 * {@code EventResubmission}). Suspending a studio disables its organization.
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

	@ApplicationModuleListener
	void on(TenantRegistered event) {
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
			throw new IllegalStateException("User " + admin.email() + " already belongs to studio(s) " + otherStudios
					+ " and cannot become admin of '" + slug + "'");
		}
		return user;
	}

}
