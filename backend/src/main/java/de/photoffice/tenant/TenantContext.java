package de.photoffice.tenant;

import java.util.Optional;

/**
 * Holds the tenant of the current unit of work.
 * <p>
 * Every database connection obtained while a tenant is bound is restricted to that tenant's rows by
 * PostgreSQL row-level security (see {@code V2__tenant.sql}). Without a bound tenant no tenant-owned rows are
 * visible at all.
 * <p>
 * The tenant must be bound <em>before</em> a transaction starts: a connection keeps the tenant it was
 * obtained with. The binding is not inherited by other threads – asynchronous event listeners must take the
 * tenant from the event and bind it themselves.
 */
public final class TenantContext {

	private static final ScopedValue<TenantId> CURRENT = ScopedValue.newInstance();

	private TenantContext() {
	}

	public static Optional<TenantId> current() {
		return CURRENT.isBound() ? Optional.of(CURRENT.get()) : Optional.empty();
	}

	public static TenantId require() {
		return current().orElseThrow(() -> new IllegalStateException("No tenant bound to the current context"));
	}

	public static void runAs(TenantId tenantId, Runnable action) {
		ScopedValue.where(CURRENT, tenantId).run(action);
	}

	public static <T, X extends Throwable> T callAs(TenantId tenantId, ScopedValue.CallableOp<? extends T, X> action)
			throws X {
		return ScopedValue.where(CURRENT, tenantId).call(action);
	}

}
