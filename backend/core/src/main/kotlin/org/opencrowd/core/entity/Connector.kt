package org.opencrowd.core.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "connectors")
class Connector(

    @Column(name = "connector_type", nullable = false)
    var connectorType: String,

    @Column(name = "name", nullable = false)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: ConnectorStatus = ConnectorStatus.DISCONNECTED,

    @Column(name = "config", columnDefinition = "JSONB", nullable = false)
    var config: String = "{}",

    @Column(name = "last_sync_at")
    var lastSyncAt: Instant? = null,

    @Column(name = "last_health_at")
    var lastHealthAt: Instant? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status")
    var healthStatus: HealthStatus? = null,

    @Column(name = "sync_schedule")
    var syncSchedule: String? = null,

) : AuditableEntity()

enum class ConnectorStatus {
    CONNECTED,
    DISCONNECTED,
    ERROR
}

enum class HealthStatus {
    HEALTHY,
    DEGRADED,
    UNHEALTHY
}
