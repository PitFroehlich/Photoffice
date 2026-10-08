package de.photoffice.tenant;

import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class TenantDataSourceConfiguration {

	/**
	 * Wraps the application's data source so that all application access is subject to row-level security.
	 */
	@Bean
	static BeanPostProcessor tenantAwareDataSourcePostProcessor() {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) {
				if (bean instanceof DataSource dataSource && !(bean instanceof TenantAwareDataSource)) {
					return new TenantAwareDataSource(dataSource);
				}
				return bean;
			}
		};
	}

	/**
	 * Flyway must run as the schema owner, not as the restricted application role.
	 */
	@Bean
	FlywayConfigurationCustomizer flywayUsesSchemaOwner() {
		return configuration -> {
			if (configuration.getDataSource() instanceof TenantAwareDataSource tenantAware) {
				configuration.dataSource(tenantAware.getTargetDataSource());
			}
		};
	}

}
