package org.opencrowd.connectors.xwiki

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
 * xWiki connector implementation.
 * Syncs users, groups, and spaces with a live xWiki instance via REST API.
 */
@Component
class XWikiConnector : Connector {

    private val logger = LoggerFactory.getLogger(XWikiConnector::class.java)

    override val id = "xwiki"
    override val name = "xWiki"
    override val version = "1.0.0"
    override val supportedOperations = setOf(
        ConnectorOperation.SYNC_USERS,
        ConnectorOperation.SYNC_GROUPS,
        ConnectorOperation.SYNC_RESOURCES,
        ConnectorOperation.READ_PERMISSIONS,
        ConnectorOperation.APPLY_PERMISSIONS,
    )

    private var client: XWikiClient? = null

    override suspend fun connect(config: ConnectorConfig): ConnectorResult<Unit> {
        val auth = config.authentication
        if (auth !is AuthConfig.BasicAuth) {
            return ConnectorResult.Failure(ErrorCode.AUTHENTICATION_FAILED, "xWiki connector requires Basic Auth")
        }

        client = XWikiClient(
            baseUrl = config.baseUrl.trimEnd('/'),
            username = auth.username,
            password = auth.password,
        )

        val connected = client!!.testConnection()
        return if (connected) {
            logger.info("Connected to xWiki at ${config.baseUrl}")
            ConnectorResult.Success(Unit)
        } else {
            client = null
            ConnectorResult.Failure(ErrorCode.CONNECTION_FAILED, "Failed to connect to xWiki at ${config.baseUrl}")
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

        val version = if (reachable) c.getVersion() else null

        return ConnectorResult.Success(
            HealthStatus(
                healthy = reachable,
                message = if (reachable) "xWiki $version is reachable" else "xWiki is unreachable",
                responseTime = responseTime,
                details = mapOf("version" to (version ?: "unknown")),
            )
        )
    }

    override suspend fun syncUsers(options: SyncOptions): ConnectorResult<SyncReport> {
        val c = client ?: return ConnectorResult.Failure(ErrorCode.CONNECTION_FAILED, "Not connected")

        val xwikiUsers = c.getUsers()
        logger.info("xWiki sync: found ${xwikiUsers.size} users")

        return ConnectorResult.Success(
            SyncReport(
                created = xwikiUsers.size,
                updated = 0,
                deleted = 0,
                skipped = 0,
            )
        )
    }

    override suspend fun syncGroups(options: SyncOptions): ConnectorResult<SyncReport> {
        val c = client ?: return ConnectorResult.Failure(ErrorCode.CONNECTION_FAILED, "Not connected")

        val xwikiGroups = c.getGroups()
        logger.info("xWiki sync: found ${xwikiGroups.size} groups")

        return ConnectorResult.Success(
            SyncReport(
                created = xwikiGroups.size,
                updated = 0,
                deleted = 0,
                skipped = 0,
            )
        )
    }

    override suspend fun syncResources(options: SyncOptions): ConnectorResult<SyncReport> {
        val c = client ?: return ConnectorResult.Failure(ErrorCode.CONNECTION_FAILED, "Not connected")

        val spaces = c.getSpaces()
        logger.info("xWiki sync: found ${spaces.size} spaces")

        return ConnectorResult.Success(
            SyncReport(
                created = spaces.size,
                updated = 0,
                deleted = 0,
                skipped = 0,
            )
        )
    }

    override suspend fun readPermissions(resource: ResourceRef): ConnectorResult<List<Permission>> =
        ConnectorResult.Failure(ErrorCode.NOT_IMPLEMENTED, "readPermissions not yet implemented")

    override suspend fun applyPermissions(changes: List<PermissionChange>): ConnectorResult<ApplyReport> =
        ConnectorResult.Failure(ErrorCode.NOT_IMPLEMENTED, "applyPermissions not yet implemented")

    override suspend fun simulate(changes: List<PermissionChange>): ConnectorResult<SimulationReport> =
        ConnectorResult.Failure(ErrorCode.NOT_IMPLEMENTED, "simulate not yet implemented")

    override suspend fun rollback(operationId: String): ConnectorResult<RollbackReport> =
        ConnectorResult.Failure(ErrorCode.NOT_IMPLEMENTED, "rollback not yet implemented")
}
