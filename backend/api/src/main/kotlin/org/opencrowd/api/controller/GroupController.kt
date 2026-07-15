package org.opencrowd.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencrowd.api.dto.ApiResponse
import org.opencrowd.api.dto.CreateGroupRequest
import org.opencrowd.api.dto.GroupResponse
import org.opencrowd.api.dto.PageResponse
import org.opencrowd.core.entity.Group
import org.opencrowd.core.entity.GroupType
import org.opencrowd.core.service.GroupService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
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
class GroupController(
    private val groupService: GroupService,
    private val groupRepository: org.opencrowd.core.repository.GroupRepository,
) {

    @GetMapping
    @Operation(summary = "List groups", description = "Returns a paginated list of groups")
    fun listGroups(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) type: String?,
        @RequestParam(defaultValue = "createdAt,desc") sort: String,
    ): ResponseEntity<PageResponse<GroupResponse>> {
        val pageable = PageRequest.of(page, size.coerceAtMost(100), Sort.by(Sort.Direction.DESC, "createdAt"))

        val result = when {
            type != null -> groupService.findByType(GroupType.valueOf(type.uppercase()), pageable)
            else -> groupService.findAll(pageable)
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
    @Operation(summary = "Get group by ID")
    fun getGroup(@PathVariable id: UUID): ResponseEntity<ApiResponse<GroupResponse>> {
        val group = groupService.findById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(ApiResponse(data = group.toResponse()))
    }

    @PostMapping
    @Operation(summary = "Create group")
    @PreAuthorize("hasRole('manage_groups')")
    fun createGroup(
        @Valid @RequestBody request: CreateGroupRequest
    ): ResponseEntity<ApiResponse<GroupResponse>> {
        val group = Group(
            name = request.name,
            description = request.description,
            type = GroupType.valueOf((request.type ?: "STATIC").uppercase()),
            parentId = request.parentId,
            ownerId = request.ownerId,
        )

        val created = groupService.create(group)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse(data = created.toResponse()))
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Add members to group")
    @PreAuthorize("hasRole('manage_groups')")
    fun addMembers(
        @PathVariable id: UUID,
        @RequestBody body: Map<String, List<String>>
    ): ResponseEntity<Void> {
        val userIds = body["userIds"] ?: throw IllegalArgumentException("Missing 'userIds' field")
        userIds.forEach { userId ->
            groupService.addMember(id, UUID.fromString(userId))
        }
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Operation(summary = "Remove member from group")
    @PreAuthorize("hasRole('manage_groups')")
    fun removeMember(
        @PathVariable id: UUID,
        @PathVariable userId: UUID
    ): ResponseEntity<Void> {
        groupService.removeMember(id, userId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "List group members")
    fun listMembers(@PathVariable id: UUID): ResponseEntity<Map<String, Any>> {
        val members = groupService.getMembers(id)
        return ResponseEntity.ok(mapOf("memberIds" to members, "count" to members.size))
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete group")
    @PreAuthorize("hasRole('manage_groups')")
    fun deleteGroup(@PathVariable id: UUID): ResponseEntity<Void> {
        groupService.delete(id)
        return ResponseEntity.noContent().build()
    }

    private fun Group.toResponse() = GroupResponse(
        id = id!!,
        name = name,
        description = description,
        type = type.name,
        parentId = parentId,
        ownerId = ownerId,
        memberCount = groupRepository.countMembers(id!!).toInt(),
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )
}
