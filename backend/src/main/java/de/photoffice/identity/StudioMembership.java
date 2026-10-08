package de.photoffice.identity;

import de.photoffice.tenant.Tenant;
import de.photoffice.tenant.TenantManagement;
import de.photoffice.tenant.TenantStatus;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Determines the studio of an authenticated user from the Keycloak {@code organization} claim.
 * <p>
 * Convention: the Keycloak organization alias equals the tenant slug. A user must belong to exactly one
 * organization, and the studio must be active.
 */
@Component
class StudioMembership {

	private static final String REQUEST_ATTRIBUTE = StudioMembership.class.getName() + ".studio";

	private final TenantManagement tenantManagement;

	StudioMembership(TenantManagement tenantManagement) {
		this.tenantManagement = tenantManagement;
	}

	/**
	 * Resolves the studio once per request (used by authorization and by tenant binding).
	 */
	@SuppressWarnings("unchecked")
	Optional<Tenant> studioOf(Authentication authentication, HttpServletRequest request) {
		Object cached = request.getAttribute(REQUEST_ATTRIBUTE);
		if (cached instanceof Optional<?> studio) {
			return (Optional<Tenant>) studio;
		}
		Optional<Tenant> studio = studioOf(authentication);
		request.setAttribute(REQUEST_ATTRIBUTE, studio);
		return studio;
	}

	Optional<Tenant> studioOf(Authentication authentication) {
		if (!(authentication instanceof JwtAuthenticationToken token)) {
			return Optional.empty();
		}
		List<String> organizations = organizations(token.getToken().getClaims().get("organization"));
		if (organizations.size() != 1) {
			return Optional.empty();
		}
		return tenantManagement.findBySlug(organizations.getFirst())
			.filter(tenant -> tenant.status() == TenantStatus.ACTIVE);
	}

	/**
	 * Keycloak sends either a list of aliases or, with "Add organization id", a map keyed by alias.
	 */
	private static List<String> organizations(Object claim) {
		if (claim instanceof Collection<?> aliases) {
			return aliases.stream().map(String::valueOf).toList();
		}
		if (claim instanceof Map<?, ?> byAlias) {
			return byAlias.keySet().stream().map(String::valueOf).toList();
		}
		return List.of();
	}

}
