package org.opencrowd.core.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "access_entries")
class AccessEntry(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    var id: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "principal_type", nullable = false)
    var principalType: PrincipalType,

    @Column(name = "principal_name", nullable = false)
    var principalName: String,

    @Column(name = "application", nullable = false)
    var application: String,

    @Column(name = "resource_type", nullable = false)
    var resourceType: String,

    @Column(name = "resource_name", nullable = false)
    var resourceName: String,

    @Column(name = "permission", nullable = false)
    var permission: String,

    @Column(name = "allow", nullable = false)
    var allow: Boolean = true,

    @Column(name = "source", nullable = false)
    var source: String = "synced",

    @Column(name = "connector_id")
    var connectorId: UUID? = null,

    @Column(name = "synced_at", nullable = false)
    var syncedAt: Instant = Instant.now(),

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
)

enum class PrincipalType {
    USER,
    GROUP
}
