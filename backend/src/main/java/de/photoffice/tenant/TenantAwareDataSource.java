package de.photoffice.tenant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Prepares every connection for row-level security: switches to the application role and sets the tenant of
 * the current {@link TenantContext} (or none).
 * <p>
 * Both settings are applied on every checkout from the pool, so a pooled connection never keeps the tenant of a
 * previous use.
 */
class TenantAwareDataSource extends DelegatingDataSource {

	static final String APPLICATION_ROLE = "photoffice_app";

	TenantAwareDataSource(DataSource targetDataSource) {
		super(targetDataSource);
	}

	@Override
	public Connection getConnection() throws SQLException {
		return prepare(super.getConnection());
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return prepare(super.getConnection(username, password));
	}

	private Connection prepare(Connection connection) throws SQLException {
		String tenantId = TenantContext.current().map(TenantId::toString).orElse("");
		try {
			try (Statement statement = connection.createStatement()) {
				statement.execute("SET ROLE " + APPLICATION_ROLE);
			}
			try (PreparedStatement statement = connection.prepareStatement("SELECT set_config('app.tenant_id', ?, false)")) {
				statement.setString(1, tenantId);
				statement.execute();
			}
			return connection;
		}
		catch (SQLException | RuntimeException ex) {
			connection.close();
			throw ex;
		}
	}

}
