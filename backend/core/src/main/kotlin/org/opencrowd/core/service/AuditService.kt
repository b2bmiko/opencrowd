package org.opencrowd.core.service

import org.opencrowd.core.entity.AuditEvent
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.Instant
import java.util.UUID

interface AuditService {

    fun findById(id: UUID): AuditEvent?

    fun findAll(pageable: Pageable): Page<AuditEvent>

    fun findByEventType(eventType: String, pageable: Pageable): Page<AuditEvent>

    fun findByActorId(actorId: UUID, pageable: Pageable): Page<AuditEvent>

    fun findByTarget(targetType: String, targetId: UUID, pageable: Pageable): Page<AuditEvent>

    fun findByDateRange(start: Instant, end: Instant, pageable: Pageable): Page<AuditEvent>

    fun findByCorrelationId(correlationId: String): List<AuditEvent>

    fun record(event: AuditEvent): AuditEvent
}
