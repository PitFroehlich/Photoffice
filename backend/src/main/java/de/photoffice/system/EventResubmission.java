package de.photoffice.system;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.modulith.events.FailedEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Delivers failed event publications again, e.g. studio onboarding while Keycloak was unreachable.
 * <p>
 * A publication fails when its listener throws, or when it is stuck (see {@code spring.modulith.events.staleness}
 * in {@code application.yml}). Listeners must therefore be idempotent.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
class EventResubmission {

	private final FailedEventPublications failedPublications;

	private final Duration minAge;

	EventResubmission(FailedEventPublications failedPublications,
			@Value("${photoffice.events.resubmission.min-age:PT1M}") Duration minAge) {
		this.failedPublications = failedPublications;
		this.minAge = minAge;
	}

	@Scheduled(fixedDelayString = "${photoffice.events.resubmission.interval:PT5M}",
			initialDelayString = "${photoffice.events.resubmission.interval:PT5M}")
	void resubmitFailedPublications() {
		failedPublications.resubmit(ResubmissionOptions.defaults().withMinAge(minAge));
	}

}
