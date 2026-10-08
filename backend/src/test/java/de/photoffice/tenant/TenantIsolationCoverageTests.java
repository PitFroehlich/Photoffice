package de.photoffice.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import de.photoffice.TestcontainersConfiguration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Guards the convention for new tables after all migrations ran: every table is either tenant-owned
 * (column {@code tenant_id} plus {@code enable_tenant_isolation}) or explicitly listed as global.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TenantIsolationCoverageTests {

	/** Tables intentionally shared by all tenants. Extend only with a good reason. */
	private static final Set<String> GLOBAL_TABLES = Set.of("flyway_schema_history", "event_publication", "tenant");

	@Autowired
	private JdbcTemplate jdbc;

	record TableSecurity(String name, boolean hasTenantColumn, boolean rlsEnabled, boolean rlsForced,
			boolean hasPolicy) {
	}

	@Test
	void everyTableIsTenantIsolatedOrExplicitlyGlobal() {
		List<TableSecurity> tables = jdbc.query("""
				SELECT c.relname AS name,
				       EXISTS (SELECT 1 FROM information_schema.columns col
				               WHERE col.table_schema = 'public' AND col.table_name = c.relname
				                 AND col.column_name = 'tenant_id') AS has_tenant_column,
				       c.relrowsecurity AS rls_enabled,
				       c.relforcerowsecurity AS rls_forced,
				       EXISTS (SELECT 1 FROM pg_policies p
				               WHERE p.schemaname = 'public' AND p.tablename = c.relname
				                 AND p.policyname = 'tenant_isolation') AS has_policy
				FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
				WHERE n.nspname = 'public' AND c.relkind IN ('r', 'p')
				""", (rs, i) -> new TableSecurity(rs.getString("name"), rs.getBoolean("has_tenant_column"),
				rs.getBoolean("rls_enabled"), rs.getBoolean("rls_forced"), rs.getBoolean("has_policy")));

		assertThat(tables).extracting(TableSecurity::name).contains("tenant");

		assertThat(tables).allSatisfy(table -> {
			if (GLOBAL_TABLES.contains(table.name())) {
				return;
			}
			assertThat(table.hasTenantColumn())
				.as("Table '%s' needs a tenant_id column (or must be added to GLOBAL_TABLES)", table.name())
				.isTrue();
			assertThat(table.rlsEnabled() && table.rlsForced() && table.hasPolicy())
				.as("Table '%s' is not secured – call SELECT enable_tenant_isolation('%s') in its migration",
						table.name(), table.name())
				.isTrue();
		});
	}

}
