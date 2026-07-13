package org.opencrowd.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencrowd.api.dto.ApiResponse
import org.opencrowd.api.dto.CreateUserRequest
import org.opencrowd.api.dto.PageResponse
import org.opencrowd.api.dto.UpdateUserRequest
import org.opencrowd.api.dto.UserResponse
import org.opencrowd.core.entity.User
import org.opencrowd.core.entity.UserStatus
import org.opencrowd.core.service.UserService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User identity management")
class UserController(
    private val userService: UserService,
) {

    @GetMapping
    @Operation(summary = "List users", description = "Returns a paginated list of users")
    fun listUsers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) department: String?,
        @RequestParam(defaultValue = "createdAt,desc") sort: String,
    ): ResponseEntity<PageResponse<UserResponse>> {
        val pageable = buildPageable(page, size, sort)

        val result = when {
            status != null -> userService.findByStatus(UserStatus.valueOf(status.uppercase()), pageable)
            department != null -> userService.findByDepartment(department, pageable)
            else -> userService.findAll(pageable)
        }

        return ResponseEntity.ok(
            PageResponse(
                content = result.content.map { it.toResponse() },
                page = result.number,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
                hasNext = result.hasNext(),
            )
        )
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    fun getUser(@PathVariable id: UUID): ResponseEntity<ApiResponse<UserResponse>> {
        val user = userService.findById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(ApiResponse(data = user.toResponse()))
    }

    @GetMapping("/count")
    @Operation(summary = "Count users by status")
    fun countUsers(@RequestParam(required = false) status: String?): ResponseEntity<Map<String, Long>> {
        val count = if (status != null) {
            userService.countByStatus(UserStatus.valueOf(status.uppercase()))
        } else {
            userService.findAll(PageRequest.of(0, 1)).totalElements
        }
        return ResponseEntity.ok(mapOf("count" to count))
    }

    @PostMapping
    @Operation(summary = "Create user", description = "Creates a new user identity")
    @PreAuthorize("hasRole('manage_users')")
    fun createUser(
        @Valid @RequestBody request: CreateUserRequest
    ): ResponseEntity<ApiResponse<UserResponse>> {
        val user = User(
            username = request.username,
            email = request.email,
            firstName = request.firstName,
            lastName = request.lastName,
            displayName = request.displayName ?: "${request.firstName ?: ""} ${request.lastName ?: ""}".trim().ifEmpty { request.username },
            department = request.department,
            title = request.title,
            phone = request.phone,
            status = UserStatus.ACTIVE,
        )

        val created = userService.create(user)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse(data = created.toResponse()))
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update user", description = "Partially updates a user")
    @PreAuthorize("hasRole('manage_users')")
    fun updateUser(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateUserRequest
    ): ResponseEntity<ApiResponse<UserResponse>> {
        val updated = userService.update(id) { user ->
            request.firstName?.let { user.firstName = it }
            request.lastName?.let { user.lastName = it }
            request.displayName?.let { user.displayName = it }
            request.department?.let { user.department = it }
            request.title?.let { user.title = it }
            request.phone?.let { user.phone = it }
            user
        }
        return ResponseEntity.ok(ApiResponse(data = updated.toResponse()))
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Change user status", description = "Updates the user's lifecycle status")
    @PreAuthorize("hasRole('manage_users')")
    fun changeStatus(
        @PathVariable id: UUID,
        @RequestBody body: Map<String, String>
    ): ResponseEntity<ApiResponse<UserResponse>> {
        val newStatus = body["status"]
            ?: throw IllegalArgumentException("Missing 'status' field in request body")
        val updated = userService.changeStatus(id, UserStatus.valueOf(newStatus.uppercase()))
        return ResponseEntity.ok(ApiResponse(data = updated.toResponse()))
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user")
    @PreAuthorize("hasRole('manage_users')")
    fun deleteUser(@PathVariable id: UUID): ResponseEntity<Void> {
        userService.delete(id)
        return ResponseEntity.noContent().build()
    }

    private fun buildPageable(page: Int, size: Int, sort: String): PageRequest {
        val parts = sort.split(",")
        val direction = if (parts.size > 1 && parts[1].equals("asc", ignoreCase = true)) {
            Sort.Direction.ASC
        } else {
            Sort.Direction.DESC
        }
        return PageRequest.of(page, size.coerceAtMost(100), Sort.by(direction, parts[0]))
    }

    private fun User.toResponse() = UserResponse(
        id = id!!,
        username = username,
        email = email,
        firstName = firstName,
        lastName = lastName,
        displayName = displayName,
        status = status.name,
        department = department,
        title = title,
        phone = phone,
        avatarUrl = avatarUrl,
        lastLoginAt = lastLoginAt?.toString(),
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )
}
