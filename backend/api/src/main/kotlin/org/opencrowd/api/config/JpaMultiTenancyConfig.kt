package org.opencrowd.api.config

import org.opencrowd.core.multitenancy.TenantConnectionProvider
import org.opencrowd.core.multitenancy.TenantIdentifierResolver
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Registers the multi-tenancy components with Hibernate.
 */
@Configuration
class JpaMultiTenancyConfig {

    @Bean
    fun multiTenantHibernateCustomizer(
        connectionProvider: TenantConnectionProvider,
        identifierResolver: TenantIdentifierResolver
    ): HibernatePropertiesCustomizer {
        return HibernatePropertiesCustomizer { properties ->
            properties["hibernate.multi_tenancy"] = "SCHEMA"
            properties["hibernate.tenant_connection_provider"] = connectionProvider
            properties["hibernate.tenant_identifier_resolver"] = identifierResolver
        }
    }
}
