package org.opencrowd.api.config

import org.hibernate.cfg.AvailableSettings
import org.opencrowd.core.multitenancy.TenantConnectionProvider
import org.opencrowd.core.multitenancy.TenantIdentifierResolver
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Registers multi-tenancy components with Hibernate 6.x.
 * Uses SCHEMA strategy: each tenant has its own PostgreSQL schema.
 */
@Configuration
class JpaMultiTenancyConfig(
    private val connectionProvider: TenantConnectionProvider,
    private val identifierResolver: TenantIdentifierResolver,
) {

    @Bean
    fun multiTenantHibernateCustomizer(): HibernatePropertiesCustomizer {
        return HibernatePropertiesCustomizer { properties ->
            properties[AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER] = connectionProvider
            properties[AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER] = identifierResolver
        }
    }
}
