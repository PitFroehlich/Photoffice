package de.photoffice.tenant;

/**
 * Filter of the studio list by onboarding state.
 */
public enum OnboardingFilter {

	/** Not completed yet, including failed attempts (they are retried). */
	PENDING,

	/** Not completed and the last attempt failed. */
	FAILED,

	COMPLETED

}
