package de.photoffice.tenant;

import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class TenantDataSourceConfiguration {

	/**
	 * Wraps the application's data source so that all application access is subject to row-level security.
	 * Liquibase is the exception: it must run as the schema owner, not as the restricted application role.
	 */
	@Bean
	static BeanPostProcessor tenantAwareDataSourcePostProcessor() {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) {
				// Runs before SpringLiquibase applies the changelog in afterPropertiesSet()
				if (bean instanceof SpringLiquibase liquibase
						&& liquibase.getDataSource() instanceof TenantAwareDataSource tenantAware) {
					liquibase.setDataSource(tenantAware.getTargetDataSource());
				}
				return bean;
			}

			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) {
				if (bean instanceof DataSource dataSource && !(bean instanceof TenantAwareDataSource)) {
					return new TenantAwareDataSource(dataSource);
				}
				return bean;
			}
		};
	}

}
