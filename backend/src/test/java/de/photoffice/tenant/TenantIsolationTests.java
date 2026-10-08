package de.photoffice.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import de.photoffice.TestcontainersConfiguration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Verifies row-level security on a probe table secured exactly like business tables
 * ({@code enable_tenant_isolation}). Queries deliberately contain no tenant filter.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TenantIsolationTests {

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private PostgreSQLContainer postgres;

	private JdbcTemplate schemaOwner;

	private final TenantId studioA = TenantId.of(UUID.randomUUID());

	private final TenantId studioB = TenantId.of(UUID.randomUUID());

	@BeforeEach
	void createProbeTable() {
		schemaOwner = new JdbcTemplate(
				new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
		schemaOwner.execute("""
				CREATE TABLE isolation_probe (
				    id UUID PRIMARY KEY,
				    tenant_id UUID NOT NULL,
				    note TEXT NOT NULL)""");
		schemaOwner.execute("SELECT enable_tenant_isolation('isolation_probe')");
	}

	@AfterEach
	void dropProbeTable() {
		schemaOwner.execute("DROP TABLE isolation_probe");
	}

	@Test
	void applicationRunsAsRestrictedRole() {
		assertThat(jdbc.queryForObject("SELECT current_user", String.class)).isEqualTo("photoffice_app");
	}

	@Test
	void eachTenantSeesOnlyItsOwnRowsEvenWithoutFilter() {
		TenantContext.runAs(studioA, () -> insert(studioA, "note of A"));
		TenantContext.runAs(studioB, () -> insert(studioB, "note of B"));

		assertThat(TenantContext.callAs(studioA, this::allNotes)).containsExactly("note of A");
		assertThat(TenantContext.callAs(studioB, this::allNotes)).containsExactly("note of B");
	}

	@Test
	void withoutTenantNoRowsAreVisible() {
		insertAsSchemaOwner(studioA, "note of A");

		assertThat(allNotes()).isEmpty();
	}

	@Test
	void pooledConnectionDoesNotKeepThePreviousTenant() {
		insertAsSchemaOwner(studioA, "note of A");

		assertThat(TenantContext.callAs(studioA, this::allNotes)).hasSize(1);
		assertThat(allNotes()).isEmpty();
	}

	@Test
	void cannotInsertRowsForAnotherTenant() {
		assertThatExceptionOfType(DataAccessException.class)
			.isThrownBy(() -> TenantContext.runAs(studioA, () -> insert(studioB, "smuggled")));
	}

	@Test
	void cannotInsertWithoutTenant() {
		assertThatExceptionOfType(DataAccessException.class).isThrownBy(() -> insert(studioA, "no tenant bound"));
	}

	@Test
	void cannotUpdateOrDeleteRowsOfAnotherTenant() {
		insertAsSchemaOwner(studioB, "note of B");

		int updated = TenantContext.callAs(studioA, () -> jdbc.update("UPDATE isolation_probe SET note = 'changed'"));
		int deleted = TenantContext.callAs(studioA, () -> jdbc.update("DELETE FROM isolation_probe"));

		assertThat(updated).isZero();
		assertThat(deleted).isZero();
		assertThat(schemaOwner.queryForList("SELECT note FROM isolation_probe", String.class)).containsExactly("note of B");
	}

	private void insert(TenantId tenant, String note) {
		jdbc.update("INSERT INTO isolation_probe (id, tenant_id, note) VALUES (?, ?, ?)", UUID.randomUUID(),
				tenant.value(), note);
	}

	private void insertAsSchemaOwner(TenantId tenant, String note) {
		schemaOwner.update("INSERT INTO isolation_probe (id, tenant_id, note) VALUES (?, ?, ?)", UUID.randomUUID(),
				tenant.value(), note);
	}

	private List<String> allNotes() {
		return jdbc.queryForList("SELECT note FROM isolation_probe", String.class);
	}

}
