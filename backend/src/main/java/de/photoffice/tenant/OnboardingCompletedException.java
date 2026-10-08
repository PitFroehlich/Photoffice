package de.photoffice.tenant;

/**
 * The data of the first studio admin can only be changed while the studio's onboarding is not complete: afterwards
 * the admin exists in Keycloak and the data is no longer kept at the studio.
 */
public class OnboardingCompletedException extends RuntimeException {

	OnboardingCompletedException(Tenant tenant) {
		super("Das Onboarding von „" + tenant.name() + "“ ist bereits abgeschlossen – die Daten des ersten Studio-Admins "
				+ "lassen sich nicht mehr ändern.");
	}

}
