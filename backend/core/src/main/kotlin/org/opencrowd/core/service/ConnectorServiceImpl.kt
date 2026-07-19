package org.opencrowd.core.service

import org.opencrowd.core.entity.Connector
import org.opencrowd.core.entity.ConnectorStatus
import org.opencrowd.core.entity.HealthStatus
import org.opencrowd.core.event.ConnectorHealthChanged
import org.opencrowd.core.event.DomainEventPublisher
import org.opencrowd.core.multitenancy.TenantContext
import org.opencrowd.core.repository.ConnectorRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class ConnectorServiceImpl(
    private val connectorRepository: ConnectorRepository,
    private val eventPublisher: DomainEventPublisher,
) : ConnectorService {

    private val logger = LoggerFactory.getLogger(ConnectorServiceImpl::class.java)

    @Transactional(readOnly = true)
    override fun findById(id: UUID): Connector? = connectorRepository.findById(id).orElse(null)

    @Transactional(readOnly = true)
    override fun findAll(): List<Connector> = connectorRepository.findAll()

    @Transactional(readOnly = true)
    override fun findByType(connectorType: String): List<Connector> =
        connectorRepository.findByConnectorType(connectorType)

    @Transactional(readOnly = true)
    override fun findByStatus(status: ConnectorStatus): List<Connector> =
        connectorRepository.findByStatus(status)

    override fun create(connector: Connector): Connector {
        connectorRepository.findByName(connector.name)?.let {
            throw IllegalArgumentException("Connector with name '${connector.name}' already exists")
        }

        val saved = connectorRepository.save(connector)
        logger.info("Connector created: id=${saved.id}, name=${saved.name}, type=${saved.connectorType}")
        return saved
    }

    override fun update(id: UUID, updater: (Connector) -> Connector): Connector {
        val existing = connectorRepository.findById(id)
            .orElseThrow { NoSuchElementException("Connector not found: $id") }

        val updated = updater(existing)
        val saved = connectorRepository.save(updated)
        logger.info("Connector updated: id=${saved.id}, name=${saved.name}")
        return saved
    }

    override fun delete(id: UUID) {
        val connector = connectorRepository.findById(id)
            .orElseThrow { NoSuchElementException("Connector not found: $id") }
        connectorRepository.delete(connector)
        logger.info("Connector deleted: id=$id, name=${connector.name}")
    }

    override fun updateHealthStatus(id: UUID, healthy: Boolean) {
        val connector = connectorRepository.findById(id)
            .orElseThrow { NoSuchElementException("Connector not found: $id") }

        val previousHealth = connector.healthStatus?.name
        val newHealth = if (healthy) HealthStatus.HEALTHY else HealthStatus.UNHEALTHY

        connector.healthStatus = newHealth
        connector.lastHealthAt = Instant.now()
        if (healthy && connector.status != ConnectorStatus.CONNECTED) {
            connector.status = ConnectorStatus.CONNECTED
        }
        if (!healthy && connector.status == ConnectorStatus.CONNECTED) {
            connector.status = ConnectorStatus.ERROR
        }
        connectorRepository.save(connector)

        logger.info("Connector health updated: id=$id, health=$newHealth")

        eventPublisher.publish(
            ConnectorHealthChanged(
                tenantId = TenantContext.getTenantId() ?: "unknown",
                actorId = null,
                correlationId = UUID.randomUUID().toString(),
                connectorId = id,
                previousHealth = previousHealth,
                newHealth = newHealth.name,
            )
        )
    }
}
