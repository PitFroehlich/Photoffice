package de.photoffice.identity;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.photoffice.tenant.InitialStudioAdmin;
import de.photoffice.tenant.Tenant;
import de.photoffice.tenant.TenantId;
import de.photoffice.tenant.TenantManagement;
import de.photoffice.tenant.TenantRegistered;
import de.photoffice.tenant.TenantRenamed;
import de.photoffice.tenant.TenantStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The onboarding uses the current data of the first studio admin from the studio (corrected by the platform operator),
 * and the event copy only for publications from before issue #42 (ADR 0012).
 */
class StudioOnboardingAdminDataTests {

	private final KeycloakAdminClient keycloak = mock();

	private final TenantManagement tenantManagement = mock();

	private final Tenant tenant = mock();

	private final StudioOnboarding onboarding = new StudioOnboarding(keycloak, tenantManagement);

	private final TenantId tenantId = TenantId.of(UUID.randomUUID());

	@BeforeEach
	void pendingStudio() {
		when(tenant.id()).thenReturn(tenantId);
		when(tenant.slug()).thenReturn("studio-x");
		when(tenant.name()).thenReturn("Studio X");
		when(tenant.status()).thenReturn(TenantStatus.ACTIVE);
		when(tenantManagement.findById(tenantId)).thenReturn(Optional.of(tenant));
		when(keycloak.findOrganization("studio-x")).thenReturn(Optional.empty());
		when(keycloak.createOrganization("studio-x", "Studio X", true)).thenReturn("org-1");
		when(keycloak.findUserByEmail(anyString())).thenReturn(Optional.empty());
		when(keycloak.createUser(anyString(), any(), any(), any()))
			.thenAnswer(call -> new KeycloakAdminClient.User("user-1", call.getArgument(0), call.getArgument(0),
					List.of(StudioOnboarding.UPDATE_PASSWORD)));
	}

	@Test
	void usesTheCurrentDataFromTheStudio() {
		when(tenant.initialAdmin()).thenReturn(Optional.of(new InitialStudioAdmin("korrigiert@example.test", "Kai", null)));

		// Old publication with the original (wrong) address
		onboarding.on(new TenantRegistered(tenantId, "studio-x", "Studio X",
				new InitialStudioAdmin("falsch@example.test", null, null)));

		verify(keycloak).createUser(eq("korrigiert@example.test"), eq("Kai"), isNull(), any());
		verify(keycloak, never()).findUserByEmail("falsch@example.test");
		verify(tenantManagement).markOnboarded(tenantId);
	}

	@Test
	void fallsBackToTheEventForPublicationsFromBeforeTheChange() {
		when(tenant.initialAdmin()).thenReturn(Optional.empty());

		onboarding.on(new TenantRegistered(tenantId, "studio-x", "Studio X",
				new InitialStudioAdmin("alt@example.test", "Alma", "Alt")));

		verify(keycloak).createUser(eq("alt@example.test"), eq("Alma"), eq("Alt"), any());
		verify(tenantManagement).markOnboarded(tenantId);
	}

	@Test
	void failsWithAReasonWithoutAnyAdminData() {
		when(tenant.initialAdmin()).thenReturn(Optional.empty());

		assertThatExceptionOfType(StudioOnboarding.OnboardingFailedException.class)
			.isThrownBy(() -> onboarding.on(new TenantRegistered(tenantId, "studio-x", "Studio X")));

		verify(tenantManagement).recordOnboardingFailure(tenantId,
				"Für das Studio ist kein erster Studio-Admin hinterlegt. Bitte die E-Mail-Adresse im Studio ergänzen.");
		verify(keycloak, never()).createOrganization(anyString(), anyString(), eq(true));
	}

	@Test
	void renamingUpdatesTheOrganizationDescriptionWithTheCurrentName() {
		when(keycloak.findOrganization("studio-x"))
			.thenReturn(Optional.of(new KeycloakAdminClient.Organization("org-1", "studio-x", "studio-x", "Alt", true)));

		// A late delivery of an older rename still writes the current name
		onboarding.on(new TenantRenamed(tenantId, "studio-x", "Zwischenname"));

		verify(keycloak).setOrganizationDescription("org-1", "Studio X");
	}

	@Test
	void renamingBeforeOnboardingDoesNothingInKeycloak() {
		onboarding.on(new TenantRenamed(tenantId, "studio-x", "Studio X"));

		verify(keycloak, never()).setOrganizationDescription(anyString(), anyString());
	}

}
