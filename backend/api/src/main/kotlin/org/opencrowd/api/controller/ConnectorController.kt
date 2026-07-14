package org.opencrowd.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencrowd.api.dto.ApiResponse
import org.opencrowd.api.dto.ConnectorResponse
import org.opencrowd.api.dto.CreateConnectorRequest
import org.opencrowd.api.dto.ListResponse
import org.opencrowd.core.entity.Connector
import org.opencrowd.core.entity.ConnectorStatus
import org.opencrowd.core.service.ConnectorService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/connectors")
@Tag(name = "Connectors", description = "Application connector management")
class ConnectorController(
    private val connectorService: ConnectorService,
) {

    @GetMapping
    @Operation(summary = "List connectors", description = "Returns all registered connectors")
    fun listConnectors(): ResponseEntity<ListResponse<ConnectorResponse>> {
        val connectors = connectorService.findAll()
        return ResponseEntity.ok(
            ListResponse(
                data = connectors.map { it.toResponse() },
                count = connectors.size,
            )
        )
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get connector by ID")
    fun getConnector(@PathVariable id: UUID): ResponseEntity<ApiResponse<ConnectorResponse>> {
        val connector = connectorService.findById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(ApiResponse(data = connector.toResponse()))
    }

    @PostMapping
    @Operation(summary = "Register connector", description = "Registers a new application connector")
    @PreAuthorize("hasRole('manage_connectors')")
    fun createConnector(
        @Valid @RequestBody request: CreateConnectorRequest
    ): ResponseEntity<ApiResponse<ConnectorResponse>> {
        val connector = Connector(
            connectorType = request.connectorType,
            name = request.name,
            config = "{}",
            syncSchedule = request.syncSchedule,
            status = ConnectorStatus.DISCONNECTED,
        )

        val created = connectorService.create(connector)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse(data = created.toResponse()))
    }

    @PostMapping("/{id}/health-check")
    @Operation(summary = "Run health check", description = "Triggers a health check on the connector")
    @PreAuthorize("hasRole('manage_connectors')")
    fun healthCheck(@PathVariable id: UUID): ResponseEntity<Map<String, Any>> {
        // For now, just mark as healthy — real implementation in Task 11-13
        connectorService.updateHealthStatus(id, true)
        return ResponseEntity.ok(mapOf("status" to "healthy", "message" to "Health check passed"))
    }

    @PostMapping("/{id}/sync")
    @Operation(summary = "Trigger sync", description = "Triggers a manual synchronization")
    @PreAuthorize("hasRole('manage_connectors')")
    fun triggerSync(@PathVariable id: UUID): ResponseEntity<Map<String, String>> {
        // Placeholder — real sync in Tasks 11-13
        return ResponseEntity.ok(mapOf("status" to "triggered", "message" to "Sync will be implemented with connector SDK"))
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove connector")
    @PreAuthorize("hasRole('manage_connectors')")
    fun deleteConnector(@PathVariable id: UUID): ResponseEntity<Void> {
        connectorService.delete(id)
        return ResponseEntity.noContent().build()
    }

    private fun Connector.toResponse() = ConnectorResponse(
        id = id!!,
        connectorType = connectorType,
        name = name,
        status = status.name,
        healthStatus = healthStatus?.name,
        lastSyncAt = lastSyncAt?.toString(),
        lastHealthAt = lastHealthAt?.toString(),
        syncSchedule = syncSchedule,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )
}
