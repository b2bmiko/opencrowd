package org.opencrowd.core.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

/**
 * Immutable audit event. Does not extend AuditableEntity because
 * audit events are never updated after creation.
 */
@Entity
@Table(name = "audit_events")
class AuditEvent(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    var id: UUID? = null,

    @Column(name = "event_type", nullable = false)
    var eventType: String,

    @Column(name = "actor_id")
    var actorId: UUID? = null,

    @Column(name = "actor_email")
    var actorEmail: String? = null,

    @Column(name = "target_type")
    var targetType: String? = null,

    @Column(name = "target_id")
    var targetId: UUID? = null,

    @Column(name = "action", nullable = false)
    var action: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb DEFAULT '{}'")
    var details: String = "{}",

    @Column(name = "correlation_id")
    var correlationId: String? = null,

    @Column(name = "ip_address")
    var ipAddress: String? = null,

    @Column(name = "user_agent", columnDefinition = "TEXT")
    var userAgent: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
) {
    @PrePersist
    fun onPrePersist() {
        if (createdAt == Instant.EPOCH) {
            createdAt = Instant.now()
        }
    }
}
