package org.opencrowd.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencrowd.api.dto.ApiResponse
import org.opencrowd.api.dto.CreateUserRequest
import org.opencrowd.api.dto.PageResponse
import org.opencrowd.api.dto.UserResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User identity management")
class UserController {

    @GetMapping
    @Operation(summary = "List users", description = "Returns a paginated list of users")
    fun listUsers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) department: String?,
    ): ResponseEntity<PageResponse<UserResponse>> {
        // Stub — will be implemented in Milestone 1
        return ResponseEntity.ok(
            PageResponse(
                content = emptyList(),
                page = page,
                size = size,
                totalElements = 0,
                totalPages = 0,
                hasNext = false,
            )
        )
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    fun getUser(@PathVariable id: UUID): ResponseEntity<ApiResponse<UserResponse>> {
        // Stub
        return ResponseEntity.notFound().build()
    }

    @PostMapping
    @Operation(summary = "Create user", description = "Creates a new user identity")
    fun createUser(
        @Valid @RequestBody request: CreateUserRequest
    ): ResponseEntity<ApiResponse<UserResponse>> {
        // Stub
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user")
    fun deleteUser(@PathVariable id: UUID): ResponseEntity<Void> {
        // Stub
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
    }
}
