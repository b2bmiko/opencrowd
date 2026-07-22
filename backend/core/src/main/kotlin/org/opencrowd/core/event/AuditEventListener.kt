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
            details = mapEventToDetails(event),
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
        is SyncCompleted -> "sync_completed"
        is PermissionPushed -> event.action
        is GroupMembershipPushed -> event.action
    }

    private fun mapEventToTargetType(event: DomainEvent): String? = when (event) {
        is UserCreated, is UserUpdated, is UserStatusChanged -> "user"
        is GroupCreated, is GroupMemberAdded, is GroupMemberRemoved -> "group"
        is RoleAssigned, is RoleRevoked -> "role"
        is ConnectorSyncCompleted, is ConnectorHealthChanged -> "connector"
        is SyncCompleted -> "connector"
        is PermissionPushed -> "permission"
        is GroupMembershipPushed -> "group"
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
        is SyncCompleted -> event.connectorId
        is PermissionPushed -> null
        is GroupMembershipPushed -> null
    }

    private fun mapEventToDetails(event: DomainEvent): String = when (event) {
        is UserCreated -> """{"username":"${event.username}","email":"${event.email}"}"""
        is UserUpdated -> """{"userId":"${event.userId}","changes":${event.changes}}"""
        is UserStatusChanged -> """{"previousStatus":"${event.previousStatus}","newStatus":"${event.newStatus}"}"""
        is GroupCreated -> """{"name":"${event.name}"}"""
        is GroupMemberAdded -> """{"groupId":"${event.groupId}","userId":"${event.userId}"}"""
        is GroupMemberRemoved -> """{"groupId":"${event.groupId}","userId":"${event.userId}"}"""
        is RoleAssigned -> """{"roleName":"${event.roleName}"}"""
        is RoleRevoked -> """{"roleName":"${event.roleName}"}"""
        is ConnectorSyncCompleted -> """{"connectorType":"${event.connectorType}","usersSync":${event.usersSync},"groupsSync":${event.groupsSync}}"""
        is ConnectorHealthChanged -> """{"previousHealth":"${event.previousHealth}","newHealth":"${event.newHealth}"}"""
        is SyncCompleted -> """{"connector":"${event.connectorName}","usersCreated":${event.usersCreated},"usersUpdated":${event.usersUpdated},"usersErrors":${event.usersErrors},"groupsCreated":${event.groupsCreated},"membersLinked":${event.membersLinked}}"""
        is PermissionPushed -> """{"principal":"${event.principalName}","type":"${event.principalType}","permission":"${event.permission}","resource":"${event.resourceName}","app":"${event.application}","action":"${event.action}"}"""
        is GroupMembershipPushed -> """{"group":"${event.groupName}","user":"${event.username}","app":"${event.application}","action":"${event.action}"}"""
    }
}
