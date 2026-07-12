package org.opencrowd.api.dto

import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.UUID

data class CreateRoleRequest(
    @field:NotBlank(message = "Role name is required")
    val name: String,

    val description: String? = null,
    val scope: String = "GLOBAL",
    val parentId: UUID? = null,
)

data class UpdateRoleRequest(
    val name: String? = null,
    val description: String? = null,
    val scope: String? = null,
)

data class RoleResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val scope: String,
    val parentId: UUID?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
