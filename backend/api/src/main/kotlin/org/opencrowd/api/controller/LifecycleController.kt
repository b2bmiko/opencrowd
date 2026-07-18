package org.opencrowd.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.opencrowd.core.entity.User
import org.opencrowd.core.entity.UserStatus
import org.opencrowd.core.service.LifecycleService
import org.opencrowd.core.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/lifecycle")
@Tag(name = "Lifecycle", description = "Joiner/Mover/Leaver (JML) identity lifecycle management")
class LifecycleController(
    private val lifecycleService: LifecycleService,
    private val userService: UserService,
) {

    @PostMapping("/joiner")
    @Operation(summary = "Execute joiner flow", description = "Creates a user, assigns groups, and provisions to connected applications")
    @PreAuthorize("hasRole('manage_users')")
    fun executeJoiner(@RequestBody body: JoinerRequest): ResponseEntity<Map<String, Any>> {
        val user = User(
            username = body.username,
            email = body.email,
            firstName = body.firstName,
            lastName = body.lastName,
            displayName = "${body.firstName ?: ""} ${body.lastName ?: ""}".trim().ifEmpty { body.username },
            department = body.department,
            title = body.title,
            status = UserStatus.ACTIVE,
        )

        val groupIds = body.groupIds.map { UUID.fromString(it) }
        val connectorIds = body.connectorIds?.map { UUID.fromString(it) }

        val result = lifecycleService.executeJoiner(user, groupIds, connectorIds)

        return ResponseEntity.ok(mapOf(
            "success" to result.success,
            "userId" to result.userId.toString(),
            "username" to result.username,
            "groupsAssigned" to result.groupsAssigned,
            "provisioning" to result.provisioningResults.map { pr ->
                mapOf(
                    "connector" to pr.connectorName,
                    "type" to pr.connectorType,
                    "action" to pr.action,
                    "message" to (pr.message ?: ""),
                )
            },
            "errors" to result.errors,
        ))
    }

    @PostMapping("/leaver/{userId}")
    @Operation(summary = "Execute leaver flow", description = "Offboards user: removes from groups, deprovisions from connected apps")
    @PreAuthorize("hasRole('manage_users')")
    fun executeLeaver(
        @PathVariable userId: UUID,
        @RequestBody(required = false) body: LeaverRequest?
    ): ResponseEntity<Map<String, Any>> {
        val connectorIds = body?.connectorIds?.map { UUID.fromString(it) }

        val result = lifecycleService.executeLeaver(userId, connectorIds)

        return ResponseEntity.ok(mapOf(
            "success" to result.success,
            "userId" to result.userId.toString(),
            "username" to result.username,
            "groupsRemoved" to result.groupsRemoved,
            "deprovisioning" to result.deprovisioningResults.map { pr ->
                mapOf(
                    "connector" to pr.connectorName,
                    "type" to pr.connectorType,
                    "action" to pr.action,
                    "message" to (pr.message ?: ""),
                )
            },
            "errors" to result.errors,
        ))
    }

    @PostMapping("/leaver/{userId}/preview")
    @Operation(summary = "Preview leaver flow", description = "Shows what would happen without executing")
    @PreAuthorize("hasRole('manage_users')")
    fun previewLeaver(@PathVariable userId: UUID): ResponseEntity<Map<String, Any>> {
        val preview = lifecycleService.previewLeaver(userId)

        return ResponseEntity.ok(mapOf(
            "userId" to preview.userId.toString(),
            "username" to preview.username,
            "currentStatus" to preview.currentStatus.name,
            "groupsToRemove" to preview.groupsToRemove,
            "connectorsToDeprovision" to preview.connectorsToDeprovision,
        ))
    }

    @PostMapping("/reactivate/{userId}")
    @Operation(summary = "Reactivate user", description = "Re-enables an offboarded user and re-provisions to connected apps")
    @PreAuthorize("hasRole('manage_users')")
    fun reactivateUser(
        @PathVariable userId: UUID,
        @RequestBody body: ReactivateRequest
    ): ResponseEntity<Map<String, Any>> {
        val groupIds = body.groupIds.map { UUID.fromString(it) }

        val result = lifecycleService.reactivateUser(userId, groupIds)

        return ResponseEntity.ok(mapOf(
            "success" to result.success,
            "userId" to result.userId.toString(),
            "username" to result.username,
            "groupsAssigned" to result.groupsAssigned,
            "provisioning" to result.provisioningResults.map { pr ->
                mapOf(
                    "connector" to pr.connectorName,
                    "type" to pr.connectorType,
                    "action" to pr.action,
                    "message" to (pr.message ?: ""),
                )
            },
            "errors" to result.errors,
        ))
    }
}

data class JoinerRequest(
    val username: String,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val department: String? = null,
    val title: String? = null,
    val groupIds: List<String> = emptyList(),
    val connectorIds: List<String>? = null,
)

data class LeaverRequest(
    val connectorIds: List<String>? = null,
)

data class ReactivateRequest(
    val groupIds: List<String> = emptyList(),
)
