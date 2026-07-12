package org.opencrowd.core.event

import org.opencrowd.core.entity.AuditEvent
import org.opencrowd.core.repository.AuditEventRepository
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * Listens to all domain events and persists them as audit events.
 * This creates an immutable audit trail of all governance operations.
 */
@Component
class AuditEventListener(
    private val auditEventRepository: AuditEventRepository
) {

    @EventListener
    fun onDomainEvent(event: DomainEvent) {
        val auditEvent = AuditEvent(
            eventType = event::class.simpleName ?: "UnknownEvent",
            actorId = event.actorId,
            action = mapEventToAction(event),
            targetType = mapEventToTargetType(event),
            targetId = mapEventToTargetId(event),
            details = "{}",
            correlationId = event.correlationId,
        )
        auditEventRepository.save(auditEvent)
    }

    private fun mapEventToAction(event: DomainEvent): String = when (event) {
        is UserCreated -> "created"
        is UserUpdated -> "updated"
        is UserStatusChanged -> "status_changed"
        is GroupCreated -> "created"
        is GroupMemberAdded -> "member_added"
        is GroupMemberRemoved -> "member_removed"
        is RoleAssigned -> "assigned"
        is RoleRevoked -> "revoked"
        is ConnectorSyncCompleted -> "sync_completed"
        is ConnectorHealthChanged -> "health_changed"
    }

    private fun mapEventToTargetType(event: DomainEvent): String? = when (event) {
        is UserCreated, is UserUpdated, is UserStatusChanged -> "user"
        is GroupCreated, is GroupMemberAdded, is GroupMemberRemoved -> "group"
        is RoleAssigned, is RoleRevoked -> "role"
        is ConnectorSyncCompleted, is ConnectorHealthChanged -> "connector"
    }

    private fun mapEventToTargetId(event: DomainEvent): java.util.UUID? = when (event) {
        is UserCreated -> event.userId
        is UserUpdated -> event.userId
        is UserStatusChanged -> event.userId
        is GroupCreated -> event.groupId
        is GroupMemberAdded -> event.groupId
        is GroupMemberRemoved -> event.groupId
        is RoleAssigned -> event.roleId
        is RoleRevoked -> event.roleId
        is ConnectorSyncCompleted -> event.connectorId
        is ConnectorHealthChanged -> event.connectorId
    }
}
