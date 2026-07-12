package org.opencrowd.core.repository

import org.opencrowd.core.entity.Connector
import org.opencrowd.core.entity.ConnectorStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ConnectorRepository : JpaRepository<Connector, UUID> {

    fun findByConnectorType(connectorType: String): List<Connector>

    fun findByStatus(status: ConnectorStatus): List<Connector>

    fun findByName(name: String): Connector?
}
