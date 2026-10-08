package de.photoffice.identity;

import de.photoffice.tenant.TenantManagement;
import java.time.Clock;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Studio onboarding in Keycloak. Can be switched off with {@code photoffice.onboarding.enabled=false} (e.g. in
 * tests without Keycloak); registered studios then stay in onboarding status PENDING.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty(name = "photoffice.onboarding.enabled", matchIfMissing = true)
@EnableConfigurationProperties(KeycloakAdminProperties.class)
class StudioOnboardingConfiguration {

	@Bean
	KeycloakAdminClient keycloakAdminClient(KeycloakAdminProperties properties, Optional<Clock> clock) {
		return new KeycloakAdminClient(properties, clock.orElse(Clock.systemUTC()));
	}

	@Bean
	StudioOnboarding studioOnboarding(KeycloakAdminClient keycloakAdminClient, TenantManagement tenantManagement) {
		return new StudioOnboarding(keycloakAdminClient, tenantManagement);
	}

}
