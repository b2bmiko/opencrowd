package org.opencrowd.api.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.opencrowd.connectors.xwiki.XWikiClient
import org.opencrowd.core.entity.Connector
import org.opencrowd.core.entity.ConnectorStatus
import org.opencrowd.core.entity.User
import org.opencrowd.core.event.DomainEvent
import org.opencrowd.core.event.UserCreated
import org.opencrowd.core.event.UserStatusChanged
import org.opencrowd.core.repository.ConnectorRepository
import org.opencrowd.core.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

/**
 * Automatically provisions user changes to all connected applications.
 * When a user is created/updated/disabled in OpenCrowd, this service
 * pushes the change to every active connector that has stored credentials.
 */
@Service
class ProvisioningService(
    private val connectorRepository: ConnectorRepository,
    private val userRepository: UserRepository,
) {

    private val logger = LoggerFactory.getLogger(ProvisioningService::class.java)
    private val objectMapper = jacksonObjectMapper()

    /**
     * Listens to domain events and triggers provisioning automatically.
     */
    @EventListener
    fun onDomainEvent(event: DomainEvent) {
        when (event) {
            is UserCreated -> {
                logger.info("[Provisioning] UserCreated event received for user ${event.username}")
                // Set tenant context for the provisioning query
                org.opencrowd.core.multitenancy.TenantContext.setTenantId(event.tenantId)
                try {
                    val user = userRepository.findById(event.userId).orElse(null)
                    if (user != null) {
                        onUserCreated(user)
                    } else {
                        logger.warn("[Provisioning] User ${event.userId} not found in DB")
                    }
                } finally {
                    org.opencrowd.core.multitenancy.TenantContext.clear()
                }
            }
            is UserStatusChanged -> {
                if (event.newStatus == "DISABLED" || event.newStatus == "OFFBOARDED") {
                    org.opencrowd.core.multitenancy.TenantContext.setTenantId(event.tenantId)
                    try {
                        val user = userRepository.findById(event.userId).orElse(null)
                        if (user != null) {
                            onUserDisabled(user)
                        }
                    } finally {
                        org.opencrowd.core.multitenancy.TenantContext.clear()
                    }
                }
            }
            else -> { /* Other events don't trigger provisioning */ }
        }
    }

    /**
     * Called when a user is created in OpenCrowd.
     * Provisions the user to all connected apps.
     */
    @Async
    fun onUserCreated(user: User) {
        val connectors = getActiveConnectors()
        if (connectors.isEmpty()) return

        logger.info("Provisioning new user '${user.username}' to ${connectors.size} connected app(s)")

        connectors.forEach { connector ->
            try {
                val config = parseConfig(connector)
                if (config != null) {
                    provisionUserToApp(connector, config, user)
                }
            } catch (e: Exception) {
                logger.error("Failed to provision user '${user.username}' to ${connector.name}: ${e.message}")
            }
        }
    }

    /**
     * Called when a user is updated in OpenCrowd.
     * Pushes the update to all connected apps.
     */
    @Async
    fun onUserUpdated(user: User) {
        val connectors = getActiveConnectors()
        if (connectors.isEmpty()) return

        logger.info("Syncing user update '${user.username}' to ${connectors.size} connected app(s)")

        connectors.forEach { connector ->
            try {
                val config = parseConfig(connector)
                if (config != null) {
                    // For now, just log — full update sync requires connector-specific logic
                    logger.info("Would sync user '${user.username}' update to ${connector.name}")
                }
            } catch (e: Exception) {
                logger.error("Failed to sync user '${user.username}' to ${connector.name}: ${e.message}")
            }
        }
    }

    /**
     * Called when a user is disabled/offboarded in OpenCrowd.
     * Disables the user in all connected apps.
     */
    @Async
    fun onUserDisabled(user: User) {
        val connectors = getActiveConnectors()
        if (connectors.isEmpty()) return

        logger.info("Disabling user '${user.username}' in ${connectors.size} connected app(s)")

        connectors.forEach { connector ->
            try {
                val config = parseConfig(connector)
                if (config != null) {
                    disableUserInApp(connector, config, user)
                }
            } catch (e: Exception) {
                logger.error("Failed to disable user '${user.username}' in ${connector.name}: ${e.message}")
            }
        }
    }

    private fun getActiveConnectors(): List<Connector> {
        return connectorRepository.findByStatus(ConnectorStatus.CONNECTED)
    }

    private fun parseConfig(connector: Connector): ConnectorCredentials? {
        return try {
            if (connector.config == "{}") return null
            objectMapper.readValue<ConnectorCredentials>(connector.config)
        } catch (e: Exception) {
            logger.warn("Failed to parse config for connector ${connector.name}: ${e.message}")
            null
        }
    }

    private fun provisionUserToApp(connector: Connector, config: ConnectorCredentials, user: User) {
        when (connector.connectorType) {
            "xwiki" -> provisionToXWiki(config, user)
            "openproject" -> logger.info("OpenProject provisioning not yet implemented")
            "nextcloud" -> logger.info("Nextcloud provisioning not yet implemented")
            else -> logger.warn("Unknown connector type: ${connector.connectorType}")
        }
    }

    private fun disableUserInApp(connector: Connector, config: ConnectorCredentials, user: User) {
        when (connector.connectorType) {
            "xwiki" -> logger.info("xWiki user disable not yet implemented for ${user.username}")
            "openproject" -> logger.info("OpenProject user disable not yet implemented")
            "nextcloud" -> logger.info("Nextcloud user disable not yet implemented")
            else -> logger.warn("Unknown connector type: ${connector.connectorType}")
        }
    }

    private fun provisionToXWiki(config: ConnectorCredentials, user: User) {
        val client = XWikiClient(
            baseUrl = config.baseUrl.trimEnd('/'),
            username = config.username,
            password = config.password,
        )

        val created = client.createUser(
            username = user.username,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            password = null,
        )

        if (created) {
            logger.info("User '${user.username}' provisioned to xWiki at ${config.baseUrl}")
        } else {
            logger.error("Failed to provision user '${user.username}' to xWiki")
        }
    }
}

data class ConnectorCredentials(
    val baseUrl: String,
    val username: String,
    val password: String,
)
