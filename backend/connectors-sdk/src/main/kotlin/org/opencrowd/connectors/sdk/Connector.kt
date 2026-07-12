package org.opencrowd.connectors.sdk

/**
 * Core interface that all OpenCrowd connectors must implement.
 * Defines the contract for synchronizing identities, groups, and
 * permissions with external collaboration platforms.
 */
interface Connector {
    val id: String
    val name: String
    val version: String
    val supportedOperations: Set<ConnectorOperation>

    // Lifecycle
    suspend fun connect(config: ConnectorConfig): ConnectorResult<Unit>
    suspend fun disconnect(): ConnectorResult<Unit>
    suspend fun healthCheck(): ConnectorResult<HealthStatus>

    // Synchronization
    suspend fun syncUsers(options: SyncOptions): ConnectorResult<SyncReport>
    suspend fun syncGroups(options: SyncOptions): ConnectorResult<SyncReport>
    suspend fun syncResources(options: SyncOptions): ConnectorResult<SyncReport>

    // Permissions
    suspend fun readPermissions(resource: ResourceRef): ConnectorResult<List<Permission>>
    suspend fun applyPermissions(changes: List<PermissionChange>): ConnectorResult<ApplyReport>
    suspend fun simulate(changes: List<PermissionChange>): ConnectorResult<SimulationReport>
    suspend fun rollback(operationId: String): ConnectorResult<RollbackReport>
}

enum class ConnectorOperation {
    SYNC_USERS,
    SYNC_GROUPS,
    SYNC_RESOURCES,
    READ_PERMISSIONS,
    APPLY_PERMISSIONS,
    SIMULATE,
    ROLLBACK
}
