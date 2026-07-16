package org.opencrowd.core.service

import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import javax.sql.DataSource

/**
 * Provisions new tenant schemas in PostgreSQL.
 * Creates the schema, then runs the tenant migration scripts.
 */
@Service
class TenantProvisioningService(
    private val dataSource: DataSource
) {

    private val logger = LoggerFactory.getLogger(TenantProvisioningService::class.java)

    /**
     * Creates a new tenant schema and applies all tenant migrations.
     * @param slug The tenant identifier (used as schema name: tenant_<slug>)
     */
    fun provisionTenant(slug: String) {
        val schemaName = "tenant_$slug"
        logger.info("Provisioning tenant schema: $schemaName")

        dataSource.connection.use { connection ->
            // Create schema
            connection.createStatement().execute("CREATE SCHEMA IF NOT EXISTS $schemaName")
            logger.info("Schema created: $schemaName")

            // Set search path to the new schema
            connection.createStatement().execute("SET search_path TO $schemaName")

            // Run tenant migrations
            val migrationSql = loadTenantMigration()
            connection.createStatement().execute(migrationSql)
            logger.info("Tenant migrations applied to: $schemaName")

            // Reset search path
            connection.createStatement().execute("SET search_path TO public")
        }

        logger.info("Tenant provisioned successfully: $schemaName")
    }

    /**
     * Drops a tenant schema entirely. Use with extreme caution.
     * @param slug The tenant identifier
     */
    fun deprovisionTenant(slug: String) {
        val schemaName = "tenant_$slug"
        logger.warn("Deprovisioning tenant schema: $schemaName")

        dataSource.connection.use { connection ->
            connection.createStatement().execute("DROP SCHEMA IF EXISTS $schemaName CASCADE")
        }

        logger.warn("Tenant deprovisioned: $schemaName")
    }

    /**
     * Checks if a tenant schema exists.
     */
    fun tenantExists(slug: String): Boolean {
        val schemaName = "tenant_$slug"
        dataSource.connection.use { connection ->
            val rs = connection.createStatement().executeQuery(
                "SELECT EXISTS(SELECT 1 FROM information_schema.schemata WHERE schema_name = '$schemaName')"
            )
            rs.next()
            return rs.getBoolean(1)
        }
    }

    private fun loadTenantMigration(): String {
        val v1 = ClassPathResource("db/tenant/V1__create_tenant_schema.sql")
        val v2 = ClassPathResource("db/tenant/V2__create_access_entries.sql")
        val sb = StringBuilder()
        sb.append(v1.inputStream.bufferedReader().readText())
        sb.append("\n")
        if (v2.exists()) {
            sb.append(v2.inputStream.bufferedReader().readText())
        }
        return sb.toString()
    }
}
