package de.photoffice.identity;

import de.photoffice.api.StudioApi;
import de.photoffice.api.model.CurrentStudioUser;
import de.photoffice.api.model.StudioSummary;
import de.photoffice.tenant.Tenant;
import de.photoffice.tenant.TenantContext;
import de.photoffice.tenant.TenantManagement;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class CurrentStudioUserController implements StudioApi {

	private final TenantManagement tenantManagement;

	CurrentStudioUserController(TenantManagement tenantManagement) {
		this.tenantManagement = tenantManagement;
	}

	@Override
	public ResponseEntity<CurrentStudioUser> getCurrentStudioUser() {
		JwtAuthenticationToken authentication = (JwtAuthenticationToken) SecurityContextHolder.getContext()
			.getAuthentication();
		Jwt token = authentication.getToken();
		// The studio comes from the request's tenant binding, i.e. the same source all business data uses
		Tenant studio = tenantManagement.findById(TenantContext.require()).orElseThrow();

		List<CurrentStudioUser.RolesEnum> roles = authentication.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.filter(a -> a.equals("ROLE_" + Roles.STUDIO_ADMIN) || a.equals("ROLE_" + Roles.PHOTOGRAPHER))
			.map(a -> CurrentStudioUser.RolesEnum.fromValue(a.substring("ROLE_".length())))
			.toList();

		CurrentStudioUser user = new CurrentStudioUser(authentication.getName(), roles,
				new StudioSummary(studio.id().value(), studio.slug(), studio.name()));
		user.setEmail(token.getClaimAsString("email"));
		user.setDisplayName(token.getClaimAsString("name"));
		return ResponseEntity.ok(user);
	}

}
