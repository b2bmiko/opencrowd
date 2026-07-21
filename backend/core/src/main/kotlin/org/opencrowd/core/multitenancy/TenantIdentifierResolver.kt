package org.opencrowd.core.multitenancy

import org.hibernate.cfg.AvailableSettings
import org.hibernate.context.spi.CurrentTenantIdentifierResolver
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
import org.springframework.stereotype.Component

/**
 * Resolves the current tenant identifier for Hibernate multi-tenancy.
 * Reads from TenantContext ThreadLocal which is set by TenantFilter.
 */
@Component
class TenantIdentifierResolver : CurrentTenantIdentifierResolver<String>, HibernatePropertiesCustomizer {

    companion object {
        // Default to tenant_acme for single-tenant deployments.
        // In multi-tenant mode, this should remain "public" and TenantFilter must always set the context.
        const val DEFAULT_SCHEMA = "acme"
    }

    override fun resolveCurrentTenantIdentifier(): String =
        TenantContext.getTenantId() ?: DEFAULT_SCHEMA

    override fun validateExistingCurrentSessions(): Boolean = true

    override fun customize(hibernateProperties: MutableMap<String, Any>) {
        hibernateProperties[AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER] = this
    }
}
