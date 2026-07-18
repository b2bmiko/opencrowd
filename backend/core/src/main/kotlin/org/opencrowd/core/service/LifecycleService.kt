package org.opencrowd.core.service

import org.opencrowd.core.entity.User
import org.opencrowd.core.entity.UserStatus
import java.util.UUID

/**
 * Joiner/Mover/Leaver (JML) lifecycle service.
 * Orchestrates provisioning and deprovisioning across all connected applications.
 * This is connector-agnostic — it works with any registered connector.
 */
interface LifecycleService {

    /**
     * Joiner flow: creates a user, assigns them to default groups,
     * and provisions them across all connected applications.
     *
     * @param user The user to onboard
     * @param groupIds Groups to assign the user to
     * @param connectorIds Specific connectors to provision (null = all connected)
     * @return Result with provisioning details
     */
    fun executeJoiner(user: User, groupIds: List<UUID>, connectorIds: List<UUID>? = null): JoinerResult

    /**
     * Leaver flow: sets user status to OFFBOARDED, removes from all groups,
     * and deprovisions from all connected applications.
     *
     * @param userId The user to offboard
     * @param connectorIds Specific connectors to deprovision (null = all connected)
     * @return Result with deprovisioning details
     */
    fun executeLeaver(userId: UUID, connectorIds: List<UUID>? = null): LeaverResult

    /**
     * Preview what a joiner flow would do without executing it.
     */
    fun previewJoiner(user: User, groupIds: List<UUID>, connectorIds: List<UUID>? = null): JoinerPreview

    /**
     * Preview what a leaver flow would do without executing it.
     */
    fun previewLeaver(userId: UUID, connectorIds: List<UUID>? = null): LeaverPreview

    /**
     * Re-enable a previously offboarded user (reversal).
     */
    fun reactivateUser(userId: UUID, groupIds: List<UUID>): JoinerResult
}

data class JoinerResult(
    val userId: UUID,
    val username: String,
    val groupsAssigned: List<String>,
    val provisioningResults: List<ProvisioningResult>,
    val success: Boolean,
    val errors: List<String> = emptyList(),
)

data class LeaverResult(
    val userId: UUID,
    val username: String,
    val groupsRemoved: List<String>,
    val deprovisioningResults: List<ProvisioningResult>,
    val success: Boolean,
    val errors: List<String> = emptyList(),
)

data class ProvisioningResult(
    val connectorId: UUID,
    val connectorName: String,
    val connectorType: String,
    val action: String, // "provisioned" | "deprovisioned" | "failed" | "skipped"
    val message: String? = null,
)

data class JoinerPreview(
    val user: User,
    val groupsToAssign: List<String>,
    val connectorsToProvision: List<String>,
)

data class LeaverPreview(
    val userId: UUID,
    val username: String,
    val groupsToRemove: List<String>,
    val connectorsToDeprovision: List<String>,
    val currentStatus: UserStatus,
)
