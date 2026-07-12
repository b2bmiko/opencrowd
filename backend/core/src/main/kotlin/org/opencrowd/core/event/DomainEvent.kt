package org.opencrowd.core.event

import java.time.Instant
import java.util.UUID

/**
 * Base interface for all domain events in OpenCrowd.
 * Events are persisted to the audit_events table and can be
 * published to a message broker in Phase 2.
 */
sealed interface DomainEvent {
    val tenantId: String
    val actorId: UUID?
    val timestamp: Instant
    val correlationId: String
}

// Identity events

data class UserCreated(
    override val tenantId: String,
    override val actorId: UUID?,
    override val timestamp: Instant = Instant.now(),
    override val correlationId: String,
    val userId: UUID,
    val email: String,
    val username: String,
) : DomainEvent

data class UserUpdated(
    override val tenantId: String,
    override val actorId: UUID?,
    override val timestamp: Instant = Instant.now(),
    override val correlationId: String,
    val userId: UUID,
    val changes: Map<String, Any?>,
) : DomainEvent

data class UserStatusChanged(
    override val tenantId: String,
    override val actorId: UUID?,
    override val timestamp: Instant = Instant.now(),
    override val correlationId: String,
    val userId: UUID,
    val previousStatus: String,
    val newStatus: String,
) : DomainEvent

// Group events

data class GroupCreated(
    override val tenantId: String,
    override val actorId: UUID?,
    override val timestamp: Instant = Instant.now(),
    override val correlationId: String,
    val groupId: UUID,
    val name: String,
) : DomainEvent

data class GroupMemberAdded(
    override val tenantId: String,
    override val actorId: UUID?,
    override val timestamp: Instant = Instant.now(),
    override val correlationId: String,
    val groupId: UUID,
    val userId: UUID,
) : DomainEvent

data class GroupMemberRemoved(
    override val tenantId: String,
    override val actorId: UUID?,
    override val timestamp: Instant = Instant.now(),
    override val correlationId: String,
    val groupId: UUID,
    val userId: UUID,
) : DomainEvent

// Role events

data class RoleAssigned(
    override val tenantId: String,
    override val actorId: UUID?,
    override val timestamp: Instant = Instant.now(),
    override val correlationId: String,
    val userId: UUID,
    val roleId: UUID,
    val roleName: String,
) : DomainEvent

data class RoleRevoked(
    override val tenantId: String,
    override val actorId: UUID?,
    override val timestamp: Instant = Instant.now(),
    override val correlationId: String,
    val userId: UUID,
    val roleId: UUID,
    val roleName: String,
) : DomainEvent

// Connector events

data class ConnectorSyncCompleted(
    override val tenantId: String,
    override val actorId: UUID?,
    override val timestamp: Instant = Instant.now(),
    override val correlationId: String,
    val connectorId: UUID,
    val connectorType: String,
    val usersSync: Int,
    val groupsSync: Int,
) : DomainEvent

data class ConnectorHealthChanged(
    override val tenantId: String,
    override val actorId: UUID?,
    override val timestamp: Instant = Instant.now(),
    override val correlationId: String,
    val connectorId: UUID,
    val previousHealth: String?,
    val newHealth: String,
) : DomainEvent
