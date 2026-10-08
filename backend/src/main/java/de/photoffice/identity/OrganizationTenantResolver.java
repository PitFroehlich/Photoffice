package de.photoffice.identity;

import de.photoffice.tenant.Tenant;
import de.photoffice.tenant.TenantId;
import de.photoffice.tenant.TenantResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Binds the studio of the logged-in studio user as tenant of the request.
 */
@Component
class OrganizationTenantResolver implements TenantResolver {

	private final StudioMembership studioMembership;

	OrganizationTenantResolver(StudioMembership studioMembership) {
		this.studioMembership = studioMembership;
	}

	@Override
	public Optional<TenantId> resolve(HttpServletRequest request) {
		return studioMembership.studioOf(SecurityContextHolder.getContext().getAuthentication(), request)
			.map(Tenant::id);
	}

}
