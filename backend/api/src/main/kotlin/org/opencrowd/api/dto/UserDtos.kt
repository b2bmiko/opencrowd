package org.opencrowd.api.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class CreateUserRequest(
    @field:NotBlank(message = "Username is required")
    val username: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Must be a valid email address")
    val email: String,

    val firstName: String? = null,
    val lastName: String? = null,
    val displayName: String? = null,
    val department: String? = null,
    val title: String? = null,
    val phone: String? = null,
)

data class UpdateUserRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val displayName: String? = null,
    val department: String? = null,
    val title: String? = null,
    val phone: String? = null,
)

data class UserResponse(
    val id: UUID,
    val username: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val displayName: String?,
    val status: String,
    val department: String?,
    val title: String?,
    val phone: String?,
    val avatarUrl: String?,
    val externalId: String?,
    val lastLoginAt: String?,
    val createdAt: String,
    val updatedAt: String,
)
