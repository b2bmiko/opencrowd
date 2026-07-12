package org.opencrowd.connectors.sdk

import java.time.Instant

/**
 * Options for synchronization operations.
 */
data class SyncOptions(
    val fullSync: Boolean = false,
    val since: Instant? = null,
    val dryRun: Boolean = false,
    val batchSize: Int = 100
)

/**
 * Report produced after a sync operation.
 */
data class SyncReport(
    val created: Int = 0,
    val updated: Int = 0,
    val deleted: Int = 0,
    val skipped: Int = 0,
    val errors: List<SyncError> = emptyList(),
    val duration: Long = 0
)

data class SyncError(
    val identifier: String,
    val message: String
)

/**
 * Health status of a connector.
 */
data class HealthStatus(
    val healthy: Boolean,
    val message: String? = null,
    val responseTime: Long? = null,
    val details: Map<String, Any> = emptyMap()
)

/**
 * Reference to a resource in the connected application.
 */
data class ResourceRef(
    val type: String,
    val id: String,
    val name: String? = null
)

/**
 * A permission on a resource.
 */
data class Permission(
    val resource: ResourceRef,
    val principal: String,
    val principalType: PrincipalType,
    val level: String,
    val inherited: Boolean = false
)

enum class PrincipalType {
    USER,
    GROUP
}

/**
 * A requested change to permissions.
 */
data class PermissionChange(
    val resource: ResourceRef,
    val principal: String,
    val principalType: PrincipalType,
    val level: String,
    val action: PermissionAction
)

enum class PermissionAction {
    GRANT,
    REVOKE
}

/**
 * Report from applying permission changes.
 */
data class ApplyReport(
    val applied: Int = 0,
    val failed: Int = 0,
    val errors: List<String> = emptyList(),
    val operationId: String
)

/**
 * Report from simulating permission changes.
 */
data class SimulationReport(
    val changes: List<SimulatedChange> = emptyList(),
    val conflicts: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

data class SimulatedChange(
    val resource: ResourceRef,
    val principal: String,
    val before: String?,
    val after: String?
)

/**
 * Report from rolling back an operation.
 */
data class RollbackReport(
    val reverted: Int = 0,
    val failed: Int = 0,
    val errors: List<String> = emptyList()
)
