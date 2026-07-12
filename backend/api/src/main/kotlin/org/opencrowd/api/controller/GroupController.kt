package org.opencrowd.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencrowd.api.dto.ApiResponse
import org.opencrowd.api.dto.CreateGroupRequest
import org.opencrowd.api.dto.GroupResponse
import org.opencrowd.api.dto.PageResponse
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
@RequestMapping("/api/v1/groups")
@Tag(name = "Groups", description = "Group management")
class GroupController {

    @GetMapping
    @Operation(summary = "List groups", description = "Returns a paginated list of groups")
    fun listGroups(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) type: String?,
    ): ResponseEntity<PageResponse<GroupResponse>> {
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
    @Operation(summary = "Get group by ID")
    fun getGroup(@PathVariable id: UUID): ResponseEntity<ApiResponse<GroupResponse>> {
        return ResponseEntity.notFound().build()
    }

    @PostMapping
    @Operation(summary = "Create group")
    fun createGroup(
        @Valid @RequestBody request: CreateGroupRequest
    ): ResponseEntity<ApiResponse<GroupResponse>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete group")
    fun deleteGroup(@PathVariable id: UUID): ResponseEntity<Void> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
    }
}
