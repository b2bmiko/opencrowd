package org.opencrowd.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.opencrowd.core.entity.AccessEntry
import org.opencrowd.core.entity.AccessRequest
import org.opencrowd.core.entity.AccessRequestStatus
import org.opencrowd.core.entity.AccessRequestType
import org.opencrowd.core.entity.PrincipalType
import org.opencrowd.core.multitenancy.TenantContext
import org.opencrowd.core.repository.AccessEntryRepository
import org.opencrowd.core.repository.AccessRequestRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/v1/requests")
@Tag(name = "Access Requests", description = "Access request workflow — submit, approve, reject")
class AccessRequestController(
    private val accessRequestRepository: AccessRequestRepository,
    private val accessEntryRepository: AccessEntryRepository,
) {

    private val logger = LoggerFactory.getLogger(AccessRequestController::class.java)

    @GetMapping
    @Operation(summary = "List access requests", description = "Returns all access requests with optional status filter")
    fun listRequests(
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Map<String, Any>> {
        ensureTenantContext()
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))

        val result = if (status != null) {
            accessRequestRepository.findByStatus(AccessRequestStatus.valueOf(status.uppercase()), pageable)
        } else {
            accessRequestRepository.findAll(pageable)
        }

        val data = result.content.map { it.toResponse() }
        val pendingCount = accessRequestRepository.countByStatus(AccessRequestStatus.PENDING)

        return ResponseEntity.ok(mapOf(
            "data" to data,
            "page" to result.number,
            "totalElements" to result.totalElements,
            "totalPages" to result.totalPages,
            "pendingCount" to pendingCount,
        ))
    }

    @PostMapping
    @Operation(summary = "Submit access request", description = "Submit a new access request (any authenticated user)")
    fun submitRequest(@RequestBody body: SubmitRequestBody): ResponseEntity<Map<String, Any>> {
        ensureTenantContext()

        val request = AccessRequest(
            requestorName = body.requestorName,
            requestorEmail = body.requestorEmail,
            requestType = AccessRequestType.valueOf((body.type ?: "ACCESS").uppercase()),
            application = body.application ?: "xwiki",
            resourceName = body.resourceName ?: "(global)",
            permission = body.permission,
            justification = body.justification,
            customFields = body.customFields, // JSON string
            status = AccessRequestStatus.PENDING,
        )

        val saved = accessRequestRepository.save(request)
        logger.info("[Request] New access request submitted: ${saved.requestorName} → ${saved.permission} on ${saved.resourceName}")

        return ResponseEntity.status(HttpStatus.CREATED).body(mapOf(
            "success" to true,
            "id" to saved.id.toString(),
            "message" to "Request submitted successfully",
        ))
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve access request", description = "Approves a pending request and grants the permission")
    @PreAuthorize("hasRole('manage_connectors')")
    fun approveRequest(
        @PathVariable id: UUID,
        @RequestBody(required = false) body: ReviewBody?
    ): ResponseEntity<Map<String, Any>> {
        ensureTenantContext()

        val request = accessRequestRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()

        if (request.status != AccessRequestStatus.PENDING) {
            return ResponseEntity.ok(mapOf("success" to false, "error" to "Request is not pending (status: ${request.status})"))
        }

        // Grant the permission in the Access Matrix
        val entry = AccessEntry(
            principalType = PrincipalType.USER,
            principalName = request.requestorName,
            application = request.application,
            resourceType = if (request.resourceName == "(global)") "wiki" else "space",
            resourceName = request.resourceName,
            permission = request.permission,
            allow = true,
            source = "request",
            syncedAt = Instant.now(),
        )
        accessEntryRepository.save(entry)

        // Update request status
        request.status = AccessRequestStatus.APPROVED
        request.reviewerName = body?.reviewerName ?: "Platform Admin"
        request.reviewedAt = Instant.now()
        request.reviewComment = body?.comment
        accessRequestRepository.save(request)

        logger.info("[Request] Approved: ${request.requestorName} → ${request.permission} on ${request.resourceName}")

        return ResponseEntity.ok(mapOf(
            "success" to true,
            "message" to "Request approved. Permission '${request.permission}' granted to '${request.requestorName}'.",
        ))
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject access request")
    @PreAuthorize("hasRole('manage_connectors')")
    fun rejectRequest(
        @PathVariable id: UUID,
        @RequestBody(required = false) body: ReviewBody?
    ): ResponseEntity<Map<String, Any>> {
        ensureTenantContext()

        val request = accessRequestRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()

        if (request.status != AccessRequestStatus.PENDING) {
            return ResponseEntity.ok(mapOf("success" to false, "error" to "Request is not pending"))
        }

        request.status = AccessRequestStatus.REJECTED
        request.reviewerName = body?.reviewerName ?: "Platform Admin"
        request.reviewedAt = Instant.now()
        request.reviewComment = body?.comment
        accessRequestRepository.save(request)

        logger.info("[Request] Rejected: ${request.requestorName} → ${request.permission}")

        return ResponseEntity.ok(mapOf(
            "success" to true,
            "message" to "Request rejected.",
        ))
    }

    @GetMapping("/pending-count")
    @Operation(summary = "Get pending request count")
    fun pendingCount(): ResponseEntity<Map<String, Long>> {
        ensureTenantContext()
        val count = accessRequestRepository.countByStatus(AccessRequestStatus.PENDING)
        return ResponseEntity.ok(mapOf("count" to count))
    }

    private fun ensureTenantContext() {
        if (TenantContext.getTenantId() == null) {
            TenantContext.setTenantId("acme")
        }
    }

    private fun AccessRequest.toResponse() = mapOf(
        "id" to id.toString(),
        "requestorName" to requestorName,
        "requestorEmail" to (requestorEmail ?: ""),
        "type" to requestType.name.lowercase(),
        "application" to application,
        "resourceName" to resourceName,
        "permission" to permission,
        "justification" to (justification ?: ""),
        "customFields" to (customFields ?: ""),
        "status" to status.name.lowercase(),
        "reviewerName" to (reviewerName ?: ""),
        "reviewComment" to (reviewComment ?: ""),
        "reviewedAt" to (reviewedAt?.toString() ?: ""),
        "createdAt" to createdAt.toString(),
    )
}

data class SubmitRequestBody(
    val requestorName: String,
    val requestorEmail: String? = null,
    val type: String? = "access",
    val application: String? = "xwiki",
    val resourceName: String? = "(global)",
    val permission: String,
    val justification: String? = null,
    val customFields: String? = null, // JSON string for custom fields
)

data class ReviewBody(
    val reviewerName: String? = null,
    val comment: String? = null,
)
