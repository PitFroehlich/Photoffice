package de.photoffice.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.FailedEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;

/**
 * "Erneut versuchen" resubmits only the failed registration event of the selected studio.
 */
class TenantOnboardingRetryTests {

	private final TenantRepository repository = mock();

	private final FailedEventPublications failedPublications = mock();

	private final TenantManagement tenantManagement = new TenantManagement(repository,
			mock(ApplicationEventPublisher.class), failedPublications, Optional.empty());

	@Test
	void resubmitsTheFailedRegistrationOfThisStudioOnly() {
		Tenant tenant = new Tenant("studio-retry", "Studio", Instant.now());
		when(repository.findById(tenant.id().value())).thenReturn(Optional.of(tenant));

		tenantManagement.retryOnboarding(tenant.id());

		ArgumentCaptor<ResubmissionOptions> options = ArgumentCaptor.forClass(ResubmissionOptions.class);
		verify(failedPublications).resubmit(options.capture());
		assertThat(options.getValue().getMinAge()).isEqualTo(Duration.ZERO);
		var admin = new InitialStudioAdmin("admin@example.test", null, null);
		assertThat(options.getValue().getFilter())
			.accepts(publicationOf(new TenantRegistered(tenant.id(), "studio-retry", "Studio", admin)))
			.rejects(publicationOf(new TenantRegistered(TenantId.of(UUID.randomUUID()), "other", "Other", admin)))
			.rejects(publicationOf(new TenantStatusChanged(tenant.id(), "studio-retry", TenantStatus.SUSPENDED)));
	}

	@Test
	void doesNothingForOnboardedStudios() {
		Tenant tenant = new Tenant("studio-done", "Studio", Instant.now());
		tenant.markOnboarded(Instant.now());
		when(repository.findById(tenant.id().value())).thenReturn(Optional.of(tenant));

		tenantManagement.retryOnboarding(tenant.id());

		verify(failedPublications, never()).resubmit(any());
	}

	@Test
	void unknownStudio() {
		assertThatExceptionOfType(TenantNotFoundException.class)
			.isThrownBy(() -> tenantManagement.retryOnboarding(TenantId.of(UUID.randomUUID())));
	}

	private static EventPublication publicationOf(Object event) {
		EventPublication publication = mock();
		when(publication.getEvent()).thenReturn(event);
		return publication;
	}

}
