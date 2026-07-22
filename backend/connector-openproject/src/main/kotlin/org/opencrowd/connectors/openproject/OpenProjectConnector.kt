package org.opencrowd.connectors.openproject

import org.opencrowd.connectors.sdk.ApplyReport
import org.opencrowd.connectors.sdk.AuthConfig
import org.opencrowd.connectors.sdk.Connector
import org.opencrowd.connectors.sdk.ConnectorConfig
import org.opencrowd.connectors.sdk.ConnectorOperation
import org.opencrowd.connectors.sdk.ConnectorResult
import org.opencrowd.connectors.sdk.ErrorCode
import org.opencrowd.connectors.sdk.HealthStatus
import org.opencrowd.connectors.sdk.Permission
import org.opencrowd.connectors.sdk.PermissionChange
import org.opencrowd.connectors.sdk.ResourceRef
import org.opencrowd.connectors.sdk.RollbackReport
import org.opencrowd.connectors.sdk.SimulationReport
import org.opencrowd.connectors.sdk.SyncOptions
import org.opencrowd.connectors.sdk.SyncReport
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * OpenProject connector implementation.
 * Syncs users, groups, projects, and memberships via OpenProject API v3.
 *
 * Auth: API key via Basic Auth (username="apikey", password=<key>)
 * Base URL: https://<host>/api/v3
 */
@Component
class OpenProjectConnector : Connector {

    private val logger = LoggerFactory.getLogger(OpenProjectConnector::class.java)

    override val id = "openproject"
    override val name = "OpenProject"
    override val version = "1.0.0"
    override val supportedOperations = setOf(
        ConnectorOperation.SYNC_USERS,
        ConnectorOperation.SYNC_GROUPS,
        ConnectorOperation.SYNC_RESOURCES,
        ConnectorOperation.READ_PERMISSIONS,
        ConnectorOperation.APPLY_PERMISSIONS,
    )

    private var client: OpenProjectClient? = null

    override suspend fun connect(config: ConnectorConfig): ConnectorResult<Unit> {
        val apiKey = when (val auth = config.authentication) {
            is AuthConfig.BasicAuth -> auth.password // API key stored as password
            is AuthConfig.ApiKey -> auth.key
            is AuthConfig.BearerToken -> auth.token
            else -> return ConnectorResult.Failure(ErrorCode.AUTHENTICATION_FAILED, "OpenProject requires an API key")
        }

        client = OpenProjectClient(
            baseUrl = config.baseUrl.trimEnd('/'),
            apiKey = apiKey,
        )

        val connected = client!!.testConnection()
        return if (connected) {
            logger.info("Connected to OpenProject at ${config.baseUrl}")
            ConnectorResult.Success(Unit)
        } else {
            client = null
            ConnectorResult.Failure(ErrorCode.CONNECTION_FAILED, "Failed to connect to OpenProject at ${config.baseUrl}")
        }
    }

    override suspend fun disconnect(): ConnectorResult<Unit> {
        client = null
        return ConnectorResult.Success(Unit)
    }

    override suspend fun healthCheck(): ConnectorResult<HealthStatus> {
        val c = client ?: return ConnectorResult.Success(
            HealthStatus(healthy = false, message = "Not connected")
        )

        val startTime = System.currentTimeMillis()
        val reachable = c.testConnection()
        val responseTime = System.currentTimeMillis() - startTime

        val info = if (reachable) c.getInstanceInfo() else null
        val instanceName = (info?.get("instanceName") as? String) ?: "OpenProject"

        return ConnectorResult.Success(
            HealthStatus(
                healthy = reachable,
                message = if (reachable) "$instanceName is reachable" else "OpenProject is unreachable",
                responseTime = responseTime,
                details = mapOf("instanceName" to instanceName),
            )
        )
    }

    override suspend fun syncUsers(options: SyncOptions): ConnectorResult<SyncReport> {
        val c = client ?: return ConnectorResult.Failure(ErrorCode.CONNECTION_FAILED, "Not connected")

        val users = c.getUsers()
        logger.info("OpenProject sync: found ${users.size} users")

        return ConnectorResult.Success(
            SyncReport(
                created = users.size,
                updated = 0,
                deleted = 0,
                skipped = 0,
            )
        )
    }

    override suspend fun syncGroups(options: SyncOptions): ConnectorResult<SyncReport> {
        val c = client ?: return ConnectorResult.Failure(ErrorCode.CONNECTION_FAILED, "Not connected")

        val groups = c.getGroups()
        logger.info("OpenProject sync: found ${groups.size} groups")

        return ConnectorResult.Success(
            SyncReport(
                created = groups.size,
                updated = 0,
                deleted = 0,
                skipped = 0,
            )
        )
    }

    override suspend fun syncResources(options: SyncOptions): ConnectorResult<SyncReport> {
        val c = client ?: return ConnectorResult.Failure(ErrorCode.CONNECTION_FAILED, "Not connected")

        val projects = c.getProjects()
        logger.info("OpenProject sync: found ${projects.size} projects")

        return ConnectorResult.Success(
            SyncReport(
                created = projects.size,
                updated = 0,
                deleted = 0,
                skipped = 0,
            )
        )
    }

    override suspend fun readPermissions(resource: ResourceRef): ConnectorResult<List<Permission>> {
        val c = client ?: return ConnectorResult.Failure(ErrorCode.CONNECTION_FAILED, "Not connected")

        val projectId = resource.id.toIntOrNull()
            ?: return ConnectorResult.Failure(ErrorCode.VALIDATION_ERROR, "Invalid project ID: ${resource.id}")

        val memberships = c.getMemberships(projectId)
        val permissions = memberships.map { membership ->
            Permission(
                resource = resource,
                principal = membership.principalName,
                principalType = if (membership.principalType == "GROUP")
                    org.opencrowd.connectors.sdk.PrincipalType.GROUP
                else
                    org.opencrowd.connectors.sdk.PrincipalType.USER,
                level = membership.roles.joinToString(","),
            )
        }

        return ConnectorResult.Success(permissions)
    }

    override suspend fun applyPermissions(changes: List<PermissionChange>): ConnectorResult<ApplyReport> =
        ConnectorResult.Failure(ErrorCode.NOT_IMPLEMENTED, "applyPermissions — use addMembership/removeMembership via client")

    override suspend fun simulate(changes: List<PermissionChange>): ConnectorResult<SimulationReport> =
        ConnectorResult.Failure(ErrorCode.NOT_IMPLEMENTED, "simulate not yet implemented")

    override suspend fun rollback(operationId: String): ConnectorResult<RollbackReport> =
        ConnectorResult.Failure(ErrorCode.NOT_IMPLEMENTED, "rollback not yet implemented")
}
