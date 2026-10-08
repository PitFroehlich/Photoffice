package de.photoffice.identity;

import jakarta.servlet.DispatcherType;
import java.util.function.Supplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.firewall.RequestRejectedHandler;

/**
 * Access rules of the API. Everything not explicitly allowed is denied.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

	@Bean
	SecurityFilterChain apiSecurity(HttpSecurity http, StudioMembership studioMembership) throws Exception {
		AuthorizationManager<RequestAuthorizationContext> studioMember = activeStudioMember(studioMembership);
		http.authorizeHttpRequests(requests -> requests
			// Error dispatches render the error of the original request (which was already authorized) –
			// denying them turned every error into an empty 401
			.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
			.requestMatchers("/actuator/health/**", "/actuator/info", "/api/system/info").permitAll()
			.requestMatchers("/api/platform/**").hasRole(Roles.PLATFORM_ADMIN)
			// Price list (#13): every studio member reads, only studio administrators change prices
			.requestMatchers(HttpMethod.GET, "/api/studio/price-list/**").access(studioMember)
			.requestMatchers("/api/studio/price-list/**").access(AuthorizationManagers.allOf(studioMember, AuthorityAuthorizationManager.hasRole(Roles.STUDIO_ADMIN)))
			.requestMatchers("/api/studio/**").access(studioMember)
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
	 * URLs rejected by Spring Security's firewall (e.g. {@code /api/platform/tenants//suspend}) get a 400 problem
	 * detail instead of the container's error page.
	 */
	@Bean
	RequestRejectedHandler requestRejectedHandler() {
		return new ProblemDetailRequestRejectedHandler();
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
