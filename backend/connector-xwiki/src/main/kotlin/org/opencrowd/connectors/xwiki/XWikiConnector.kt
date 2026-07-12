package org.opencrowd.connectors.xwiki

import org.opencrowd.connectors.sdk.ApplyReport
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
import org.springframework.stereotype.Component

/**
 * xWiki connector stub implementation.
 * Will be fully implemented in Milestone 1 to sync users, groups, and spaces
 * with xWiki's REST API.
 */
@Component
class XWikiConnector : Connector {

    override val id = "xwiki"
    override val name = "xWiki"
    override val version = "0.1.0"
    override val supportedOperations = setOf(
        ConnectorOperation.SYNC_USERS,
        ConnectorOperation.SYNC_GROUPS,
        ConnectorOperation.SYNC_RESOURCES,
        ConnectorOperation.READ_PERMISSIONS,
        ConnectorOperation.APPLY_PERMISSIONS,
    )

    override suspend fun connect(config: ConnectorConfig): ConnectorResult<Unit> =
        notImplemented()

    override suspend fun disconnect(): ConnectorResult<Unit> =
        notImplemented()

    override suspend fun healthCheck(): ConnectorResult<HealthStatus> =
        ConnectorResult.Success(HealthStatus(healthy = true, message = "Stub connector — not connected"))

    override suspend fun syncUsers(options: SyncOptions): ConnectorResult<SyncReport> =
        notImplemented()

    override suspend fun syncGroups(options: SyncOptions): ConnectorResult<SyncReport> =
        notImplemented()

    override suspend fun syncResources(options: SyncOptions): ConnectorResult<SyncReport> =
        notImplemented()

    override suspend fun readPermissions(resource: ResourceRef): ConnectorResult<List<Permission>> =
        notImplemented()

    override suspend fun applyPermissions(changes: List<PermissionChange>): ConnectorResult<ApplyReport> =
        notImplemented()

    override suspend fun simulate(changes: List<PermissionChange>): ConnectorResult<SimulationReport> =
        notImplemented()

    override suspend fun rollback(operationId: String): ConnectorResult<RollbackReport> =
        notImplemented()

    private fun <T> notImplemented(): ConnectorResult<T> =
        ConnectorResult.Failure(ErrorCode.NOT_IMPLEMENTED, "xWiki connector not yet implemented")
}
