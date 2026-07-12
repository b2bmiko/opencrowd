package org.opencrowd.core.service

import org.opencrowd.core.entity.Connector
import org.opencrowd.core.entity.ConnectorStatus
import java.util.UUID

interface ConnectorService {

    fun findById(id: UUID): Connector?

    fun findAll(): List<Connector>

    fun findByType(connectorType: String): List<Connector>

    fun findByStatus(status: ConnectorStatus): List<Connector>

    fun create(connector: Connector): Connector

    fun update(id: UUID, updater: (Connector) -> Connector): Connector

    fun delete(id: UUID)

    fun updateHealthStatus(id: UUID, healthy: Boolean)
}
