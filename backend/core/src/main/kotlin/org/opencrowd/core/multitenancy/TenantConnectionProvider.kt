package org.opencrowd.core.multitenancy

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider
import org.springframework.stereotype.Component
import java.sql.Connection
import javax.sql.DataSource

/**
 * Switches the database schema based on the current tenant.
 * Each tenant's data lives in a separate PostgreSQL schema: tenant_<slug>
 */
@Component
class TenantConnectionProvider(
    private val dataSource: DataSource
) : MultiTenantConnectionProvider<String> {

    override fun getAnyConnection(): Connection = dataSource.connection

    override fun releaseAnyConnection(connection: Connection) {
        connection.close()
    }

    override fun getConnection(tenantIdentifier: String): Connection {
        val connection = dataSource.connection
        val schema = if (tenantIdentifier == TenantIdentifierResolver.DEFAULT_SCHEMA) {
            "public"
        } else {
            "tenant_$tenantIdentifier"
        }
        connection.createStatement().execute("SET search_path TO $schema")
        return connection
    }

    override fun releaseConnection(tenantIdentifier: String, connection: Connection) {
        connection.createStatement().execute("SET search_path TO public")
        connection.close()
    }

    override fun supportsAggressiveRelease(): Boolean = false

    override fun isUnwrappableAs(unwrapType: Class<*>): Boolean = false

    override fun <T : Any?> unwrap(unwrapType: Class<T>): T =
        throw UnsupportedOperationException("Cannot unwrap to $unwrapType")
}
