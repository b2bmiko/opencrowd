package org.opencrowd.core.repository

import org.opencrowd.core.entity.AuditEvent
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface AuditEventRepository : JpaRepository<AuditEvent, UUID> {

    fun findByEventType(eventType: String, pageable: Pageable): Page<AuditEvent>

    fun findByActorId(actorId: UUID, pageable: Pageable): Page<AuditEvent>

    fun findByTargetTypeAndTargetId(targetType: String, targetId: UUID, pageable: Pageable): Page<AuditEvent>

    fun findByCreatedAtBetween(start: Instant, end: Instant, pageable: Pageable): Page<AuditEvent>

    fun findByCorrelationId(correlationId: String): List<AuditEvent>
}
