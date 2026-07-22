package org.opencrowd.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.opencrowd.api.dto.AuditEventResponse
import org.opencrowd.api.dto.PageResponse
import org.opencrowd.core.repository.AuditEventRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/audit-events")
@Tag(name = "Audit", description = "Audit event log")
class AuditController(
    private val auditEventRepository: AuditEventRepository,
) {

    @GetMapping
    @Operation(summary = "List audit events", description = "Returns a paginated list of audit events")
    fun listAuditEvents(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) eventType: String?,
        @RequestParam(required = false) actorId: UUID?,
        @RequestParam(required = false) targetType: String?,
        @RequestParam(required = false) targetId: UUID?,
    ): ResponseEntity<PageResponse<AuditEventResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))

        val result = when {
            eventType != null -> auditEventRepository.findByEventType(eventType, pageable)
            actorId != null -> auditEventRepository.findByActorId(actorId, pageable)
            targetType != null && targetId != null -> auditEventRepository.findByTargetTypeAndTargetId(targetType, targetId, pageable)
            else -> auditEventRepository.findAll(pageable)
        }

        return ResponseEntity.ok(
            PageResponse(
                content = result.content.map { event ->
                    AuditEventResponse(
                        id = event.id!!,
                        eventType = event.eventType,
                        actorId = event.actorId,
                        actorEmail = event.actorEmail,
                        targetType = event.targetType,
                        targetId = event.targetId,
                        action = event.action,
                        details = event.details,
                        correlationId = event.correlationId,
                        createdAt = event.createdAt.toString(),
                    )
                },
                page = result.number,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
                hasNext = result.hasNext(),
            )
        )
    }
}
