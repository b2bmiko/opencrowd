package org.opencrowd.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencrowd.api.dto.ApiResponse
import org.opencrowd.api.dto.CreateRoleRequest
import org.opencrowd.api.dto.PageResponse
import org.opencrowd.api.dto.RoleResponse
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
@RequestMapping("/api/v1/roles")
@Tag(name = "Roles", description = "Role management (RBAC)")
class RoleController {

    @GetMapping
    @Operation(summary = "List roles", description = "Returns a paginated list of roles")
    fun listRoles(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) scope: String?,
    ): ResponseEntity<PageResponse<RoleResponse>> {
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
    @Operation(summary = "Get role by ID")
    fun getRole(@PathVariable id: UUID): ResponseEntity<ApiResponse<RoleResponse>> {
        return ResponseEntity.notFound().build()
    }

    @PostMapping
    @Operation(summary = "Create role")
    fun createRole(
        @Valid @RequestBody request: CreateRoleRequest
    ): ResponseEntity<ApiResponse<RoleResponse>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete role")
    fun deleteRole(@PathVariable id: UUID): ResponseEntity<Void> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
    }
}
