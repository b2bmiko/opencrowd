package org.opencrowd.api.dto

import java.time.Instant
import java.util.UUID

data class AuditEventResponse(
    val id: UUID,
    val eventType: String,
    val actorId: UUID?,
    val actorEmail: String?,
    val targetType: String?,
    val targetId: UUID?,
    val action: String,
    val details: String?,
    val correlationId: String?,
    val createdAt: Instant,
)
