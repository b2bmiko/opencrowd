package org.opencrowd.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencrowd.api.dto.ApiResponse
import org.opencrowd.api.dto.ConnectorResponse
import org.opencrowd.api.dto.CreateConnectorRequest
import org.opencrowd.api.dto.ListResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
class ConnectorController {

    @GetMapping
    @Operation(summary = "List connectors", description = "Returns all registered connectors")
    fun listConnectors(): ResponseEntity<ListResponse<ConnectorResponse>> {
        return ResponseEntity.ok(
            ListResponse(
                data = emptyList(),
                count = 0,
            )
        )
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get connector by ID")
    fun getConnector(@PathVariable id: UUID): ResponseEntity<ApiResponse<ConnectorResponse>> {
        return ResponseEntity.notFound().build()
    }

    @PostMapping
    @Operation(summary = "Register connector", description = "Registers a new application connector")
    fun createConnector(
        @Valid @RequestBody request: CreateConnectorRequest
    ): ResponseEntity<ApiResponse<ConnectorResponse>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
    }

    @PostMapping("/{id}/sync")
    @Operation(summary = "Trigger sync", description = "Triggers a manual synchronization")
    fun triggerSync(@PathVariable id: UUID): ResponseEntity<Void> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
    }

    @GetMapping("/{id}/health")
    @Operation(summary = "Check health", description = "Returns connector health status")
    fun checkHealth(@PathVariable id: UUID): ResponseEntity<Void> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove connector")
    fun deleteConnector(@PathVariable id: UUID): ResponseEntity<Void> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
    }
}
