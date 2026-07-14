package org.opencrowd.api.dto

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class CreateGroupRequest(
    @field:NotBlank(message = "Group name is required")
    val name: String,

    val description: String? = null,
    val type: String = "STATIC",
    val parentId: UUID? = null,
    val ownerId: UUID? = null,
)

data class UpdateGroupRequest(
    val name: String? = null,
    val description: String? = null,
    val ownerId: UUID? = null,
)

data class GroupResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val type: String,
    val parentId: UUID?,
    val ownerId: UUID?,
    val memberCount: Int,
    val createdAt: String,
    val updatedAt: String,
)
