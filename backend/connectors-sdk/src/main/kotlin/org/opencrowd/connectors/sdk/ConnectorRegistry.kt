package org.opencrowd.connectors.sdk

import org.slf4j.LoggerFactory

/**
 * Registry that collects all available connectors.
 * Connectors register themselves as Spring beans and are
 * automatically discovered at startup.
 */
class ConnectorRegistry(
    private val connectors: List<Connector>
) {

    private val logger = LoggerFactory.getLogger(ConnectorRegistry::class.java)

    init {
        logger.info("ConnectorRegistry initialized with ${connectors.size} connector(s): ${connectors.map { it.id }}")
    }

    fun getAll(): List<Connector> = connectors

    fun getById(id: String): Connector? = connectors.find { it.id == id }

    fun getByName(name: String): Connector? = connectors.find { it.name == name }

    fun count(): Int = connectors.size
}
