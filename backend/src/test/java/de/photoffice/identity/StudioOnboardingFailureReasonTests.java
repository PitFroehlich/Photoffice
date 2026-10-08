package de.photoffice.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * German reasons shown to the platform operator when studio onboarding fails.
 */
class StudioOnboardingFailureReasonTests {

	@Test
	void conflictMessageIsShownAsIs() {
		assertThat(StudioOnboarding.failureReason(new StudioOnboarding.OnboardingFailedException("Konflikt")))
			.isEqualTo("Konflikt");
	}

	@Test
	void keycloakUnreachable() {
		assertThat(StudioOnboarding.failureReason(new ResourceAccessException("I/O error", new IOException())))
			.isEqualTo("Keycloak ist nicht erreichbar.");
	}

	@Test
	void keycloakRejectsRequest() {
		var forbidden = HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden", HttpHeaders.EMPTY,
				new byte[0], StandardCharsets.UTF_8);
		assertThat(StudioOnboarding.failureReason(forbidden)).isEqualTo("Keycloak hat eine Anfrage abgelehnt (HTTP 403).");
	}

	@Test
	void unexpectedError() {
		assertThat(StudioOnboarding.failureReason(new IllegalStateException("kaputt")))
			.isEqualTo("Unerwarteter Fehler: kaputt");
	}

}
