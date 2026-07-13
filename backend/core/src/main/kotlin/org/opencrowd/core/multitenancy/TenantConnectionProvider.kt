package org.opencrowd.core.multitenancy

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.sql.Connection
import javax.sql.DataSource

/**
 * Switches the PostgreSQL search_path based on the current tenant.
 * Each tenant's data lives in a separate schema: tenant_<slug>
 */
@Component
class TenantConnectionProvider(
    private val dataSource: DataSource
) : MultiTenantConnectionProvider<String> {

    private val logger = LoggerFactory.getLogger(TenantConnectionProvider::class.java)

    override fun getAnyConnection(): Connection {
        return dataSource.connection
    }

    override fun releaseAnyConnection(connection: Connection) {
        connection.close()
    }

    override fun getConnection(tenantIdentifier: String): Connection {
        val connection = dataSource.connection
        val schema = resolveSchema(tenantIdentifier)
        connection.createStatement().use { stmt ->
            stmt.execute("SET search_path TO $schema, public")
        }
        return connection
    }

    override fun releaseConnection(tenantIdentifier: String, connection: Connection) {
        connection.createStatement().use { stmt ->
            stmt.execute("SET search_path TO public")
        }
        connection.close()
    }

    override fun supportsAggressiveRelease(): Boolean = false

    override fun isUnwrappableAs(unwrapType: Class<*>): Boolean = false

    override fun <T : Any?> unwrap(unwrapType: Class<T>): T =
        throw UnsupportedOperationException("Cannot unwrap")

    private fun resolveSchema(tenantId: String): String {
        return if (tenantId == "public" || tenantId == TenantIdentifierResolver.DEFAULT_SCHEMA) {
            "public"
        } else {
            "tenant_$tenantId"
        }
    }
}
