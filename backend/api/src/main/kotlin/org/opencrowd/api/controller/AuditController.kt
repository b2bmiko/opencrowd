package org.opencrowd.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.opencrowd.api.dto.AuditEventResponse
import org.opencrowd.api.dto.PageResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/audit-events")
@Tag(name = "Audit", description = "Audit event log")
class AuditController {

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
}
