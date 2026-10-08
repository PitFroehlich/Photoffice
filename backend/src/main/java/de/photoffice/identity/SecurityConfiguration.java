package de.photoffice.identity;

import java.util.function.Supplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * Access rules of the API. Everything not explicitly allowed is denied.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

	@Bean
	SecurityFilterChain apiSecurity(HttpSecurity http, StudioMembership studioMembership) throws Exception {
		http.authorizeHttpRequests(requests -> requests
			.requestMatchers("/actuator/health/**", "/actuator/info", "/api/system/info").permitAll()
			.requestMatchers("/api/platform/**").hasRole(Roles.PLATFORM_ADMIN)
			.requestMatchers("/api/studio/**").access(activeStudioMember(studioMembership))
			.anyRequest().denyAll())
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			// Stateless bearer-token API without cookies – CSRF protection is not applicable
			.csrf(csrf -> csrf.disable())
			.httpBasic(basic -> basic.disable())
			.formLogin(form -> form.disable())
			.logout(logout -> logout.disable())
			.headers(Customizer.withDefaults());
		return http.build();
	}

	/**
	 * Studio endpoints require a studio role and membership in exactly one active studio.
	 */
	private static AuthorizationManager<RequestAuthorizationContext> activeStudioMember(
			StudioMembership studioMembership) {
		return new AuthorizationManager<>() {
			@Override
			public AuthorizationResult authorize(Supplier<? extends Authentication> authentication,
					RequestAuthorizationContext context) {
				Authentication auth = authentication.get();
				boolean hasStudioRole = auth != null && auth.isAuthenticated() && auth.getAuthorities()
					.stream()
					.map(GrantedAuthority::getAuthority)
					.anyMatch(a -> a.equals("ROLE_" + Roles.STUDIO_ADMIN) || a.equals("ROLE_" + Roles.PHOTOGRAPHER));
				return new AuthorizationDecision(
						hasStudioRole && studioMembership.studioOf(auth, context.getRequest()).isPresent());
			}
		};
	}

	private static JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
		converter.setPrincipalClaimName("preferred_username");
		return converter;
	}

}
