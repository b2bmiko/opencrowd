package org.opencrowd.api.dto

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class CreateConnectorRequest(
    @field:NotBlank(message = "Connector type is required")
    val connectorType: String,

    @field:NotBlank(message = "Connector name is required")
    val name: String,

    val config: Map<String, Any> = emptyMap(),
    val syncSchedule: String? = null,
)

data class ConnectorResponse(
    val id: UUID,
    val connectorType: String,
    val name: String,
    val status: String,
    val healthStatus: String?,
    val lastSyncAt: String?,
    val lastHealthAt: String?,
    val syncSchedule: String?,
    val createdAt: String,
    val updatedAt: String,
)
