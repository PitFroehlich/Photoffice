package de.photoffice.tenant;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * Determines the tenant of an incoming HTTP request (e.g. from the authenticated user or a gallery link).
 * The first resolver returning a tenant wins.
 */
public interface TenantResolver {

	Optional<TenantId> resolve(HttpServletRequest request);

}
