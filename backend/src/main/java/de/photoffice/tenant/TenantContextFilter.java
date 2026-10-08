package de.photoffice.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Binds the tenant resolved for the request to {@link TenantContext} for the rest of the request processing.
 */
@Component
class TenantContextFilter extends OncePerRequestFilter {

	private final List<TenantResolver> resolvers;

	TenantContextFilter(List<TenantResolver> resolvers) {
		this.resolvers = resolvers;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Optional<TenantId> tenantId = resolvers.stream()
			.map(resolver -> resolver.resolve(request))
			.flatMap(Optional::stream)
			.findFirst();
		if (tenantId.isEmpty()) {
			chain.doFilter(request, response);
			return;
		}
		try {
			TenantContext.callAs(tenantId.get(), () -> {
				try {
					chain.doFilter(request, response);
				}
				catch (IOException ex) {
					throw new UncheckedIOException(ex);
				}
				return null;
			});
		}
		catch (UncheckedIOException ex) {
			throw ex.getCause();
		}
	}

}
