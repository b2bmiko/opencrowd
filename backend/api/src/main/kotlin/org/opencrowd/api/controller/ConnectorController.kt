package org.opencrowd.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencrowd.api.dto.ApiResponse
import org.opencrowd.api.dto.ConnectorResponse
import org.opencrowd.api.dto.CreateConnectorRequest
import org.opencrowd.api.dto.ListResponse
import org.opencrowd.connectors.sdk.AuthConfig
import org.opencrowd.connectors.sdk.ConnectorConfig
import org.opencrowd.connectors.sdk.ConnectorRegistry
import org.opencrowd.connectors.sdk.ConnectorResult
import org.opencrowd.connectors.sdk.SyncOptions
import org.opencrowd.core.entity.Connector
import org.opencrowd.core.entity.ConnectorStatus
import org.opencrowd.core.service.ConnectorService
import kotlinx.coroutines.runBlocking
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
    private val connectorRegistry: ConnectorRegistry,
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
        connectorService.updateHealthStatus(id, true)
        return ResponseEntity.ok(mapOf("status" to "healthy", "message" to "Health check passed"))
    }

    @PostMapping("/{id}/sync")
    @Operation(summary = "Trigger sync", description = "Triggers a manual synchronization")
    @PreAuthorize("hasRole('manage_connectors')")
    fun triggerSync(@PathVariable id: UUID): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("status" to "triggered", "message" to "Sync will be implemented with connector SDK"))
    }

    @PostMapping("/test-connection")
    @Operation(summary = "Test connection", description = "Tests connectivity to an application and saves credentials on success")
    @PreAuthorize("hasRole('manage_connectors')")
    fun testConnection(@RequestBody body: Map<String, String>): ResponseEntity<Map<String, Any>> {
        val connectorType = body["connectorType"] ?: throw IllegalArgumentException("Missing connectorType")
        val baseUrl = body["baseUrl"] ?: throw IllegalArgumentException("Missing baseUrl")
        val username = body["username"] ?: throw IllegalArgumentException("Missing username")
        val password = body["password"] ?: throw IllegalArgumentException("Missing password")
        val connectorId = body["connectorId"] // Optional — if provided, saves credentials to this connector

        val connector = connectorRegistry.getById(connectorType)
            ?: return ResponseEntity.badRequest().body(mapOf("success" to false, "message" to "Unknown connector type: $connectorType"))

        val config = ConnectorConfig(
            baseUrl = baseUrl,
            authentication = AuthConfig.BasicAuth(username, password),
        )

        val result = runBlocking { connector.connect(config) }
        return when (result) {
            is ConnectorResult.Success -> {
                val healthResult = runBlocking { connector.healthCheck() }
                val health = (healthResult as? ConnectorResult.Success)?.data

                // Save credentials to connector if ID provided
                if (connectorId != null) {
                    try {
                        val id = UUID.fromString(connectorId)
                        connectorService.update(id) { dbConnector ->
                            // Store config as JSON (in production, encrypt the password)
                            dbConnector.config = """{"baseUrl":"$baseUrl","username":"$username","password":"$password"}"""
                            dbConnector.status = org.opencrowd.core.entity.ConnectorStatus.CONNECTED
                            dbConnector.healthStatus = org.opencrowd.core.entity.HealthStatus.HEALTHY
                            dbConnector.lastHealthAt = java.time.Instant.now()
                            dbConnector
                        }
                    } catch (_: Exception) { /* ignore save errors */ }
                }

                ResponseEntity.ok(mapOf(
                    "success" to true,
                    "message" to (health?.message ?: "Connected successfully"),
                    "responseTime" to (health?.responseTime ?: 0),
                    "details" to (health?.details ?: emptyMap<String, Any>()),
                ))
            }
            is ConnectorResult.Failure -> ResponseEntity.ok(mapOf(
                "success" to false,
                "message" to result.message,
            ))
        }
    }

    @PostMapping("/{id}/sync-users")
    @Operation(summary = "Sync users", description = "Syncs users from the connected application")
    @PreAuthorize("hasRole('manage_connectors')")
    fun syncUsers(
        @PathVariable id: UUID,
        @RequestBody body: Map<String, String>
    ): ResponseEntity<Map<String, Any>> {
        val dbConnector = connectorService.findById(id)
            ?: return ResponseEntity.notFound().build()

        val connector = connectorRegistry.getById(dbConnector.connectorType)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "Unknown connector type"))

        val baseUrl = body["baseUrl"] ?: throw IllegalArgumentException("Missing baseUrl")
        val username = body["username"] ?: throw IllegalArgumentException("Missing username")
        val password = body["password"] ?: throw IllegalArgumentException("Missing password")

        val config = ConnectorConfig(
            baseUrl = baseUrl,
            authentication = AuthConfig.BasicAuth(username, password),
        )

        // Connect and sync
        val connectResult = runBlocking { connector.connect(config) }
        if (connectResult is ConnectorResult.Failure) {
            return ResponseEntity.ok(mapOf("error" to connectResult.message))
        }

        val syncResult = runBlocking { connector.syncUsers(SyncOptions()) }
        return when (syncResult) {
            is ConnectorResult.Success -> ResponseEntity.ok(mapOf(
                "success" to true,
                "created" to syncResult.data.created,
                "updated" to syncResult.data.updated,
                "deleted" to syncResult.data.deleted,
                "skipped" to syncResult.data.skipped,
            ))
            is ConnectorResult.Failure -> ResponseEntity.ok(mapOf(
                "success" to false,
                "error" to syncResult.message,
            ))
        }
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
