package org.opencrowd.api.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.opencrowd.connectors.xwiki.XWikiClient
import org.opencrowd.core.entity.ConnectorStatus
import org.opencrowd.core.entity.Group
import org.opencrowd.core.entity.GroupType
import org.opencrowd.core.entity.User
import org.opencrowd.core.entity.UserStatus
import org.opencrowd.core.multitenancy.TenantContext
import org.opencrowd.core.service.ConnectorService
import org.opencrowd.core.service.GroupService
import org.opencrowd.core.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Scheduled auto-sync service.
 * Periodically syncs users, groups, and memberships from all connected applications
 * using their stored credentials. No manual interaction needed.
 */
@Service
class AutoSyncScheduler(
    private val connectorService: ConnectorService,
    private val userService: UserService,
    private val groupService: GroupService,
) {

    private val logger = LoggerFactory.getLogger(AutoSyncScheduler::class.java)
    private val objectMapper = jacksonObjectMapper()

    /**
     * Runs every 30 minutes. Syncs all connected xWiki instances.
     * Configurable via application.yml: opencrowd.sync.interval
     */
    @Scheduled(fixedDelayString = "\${opencrowd.sync.interval-ms:1800000}", initialDelay = 60000)
    @Transactional
    fun autoSync() {
        // Set tenant context for DB access
        TenantContext.setTenantId("acme")

        try {
            val connectors = connectorService.findAll()
                .filter { it.status == ConnectorStatus.CONNECTED && it.config != "{}" }

            if (connectors.isEmpty()) {
                logger.debug("[AutoSync] No connected connectors with credentials. Skipping.")
                return
            }

            logger.info("[AutoSync] Starting scheduled sync for ${connectors.size} connector(s)")

            connectors.forEach { connector ->
                when (connector.connectorType) {
                    "xwiki" -> syncXWiki(connector)
                    else -> logger.debug("[AutoSync] Skipping unsupported connector type: ${connector.connectorType}")
                }
            }
        } catch (e: Exception) {
            logger.error("[AutoSync] Scheduled sync failed: ${e.message}", e)
        } finally {
            TenantContext.clear()
        }
    }

    private fun syncXWiki(connector: org.opencrowd.core.entity.Connector) {
        val client = buildXWikiClient(connector) ?: return

        logger.info("[AutoSync] Syncing xWiki connector: ${connector.name}")
        val startTime = System.currentTimeMillis()

        var usersCreated = 0
        var usersUpdated = 0
        var groupsCreated = 0
        var membersLinked = 0

        try {
            // Phase 1: Sync users
            val xwikiUsers = client.getUsers()
            xwikiUsers.forEach { xwikiUser ->
                try {
                    if (xwikiUser.username in SYSTEM_USERS) return@forEach

                    val existing = userService.findByUsername(xwikiUser.username)
                    if (existing != null) {
                        val newEmail = xwikiUser.email?.takeIf { it.isNotBlank() }
                        val newFirstName = xwikiUser.firstName?.takeIf { it.isNotBlank() }
                        val newLastName = xwikiUser.lastName?.takeIf { it.isNotBlank() }

                        val emailSafe = if (newEmail != null && newEmail != existing.email) {
                            val emailOwner = userService.findByEmail(newEmail)
                            if (emailOwner == null || emailOwner.id == existing.id) newEmail else null
                        } else null

                        val hasChanges = emailSafe != null ||
                            (newFirstName != null && newFirstName != existing.firstName) ||
                            (newLastName != null && newLastName != existing.lastName)

                        if (hasChanges) {
                            userService.update(existing.id!!) { user ->
                                if (emailSafe != null) user.email = emailSafe
                                if (newFirstName != null) user.firstName = newFirstName
                                if (newLastName != null) user.lastName = newLastName
                                user.displayName = listOfNotNull(user.firstName, user.lastName).joinToString(" ").ifEmpty { user.username }
                                user
                            }
                            usersUpdated++
                        }
                    } else {
                        val email = xwikiUser.email?.takeIf { it.isNotBlank() } ?: "${xwikiUser.username}@imported.local"
                        val safeEmail = if (userService.findByEmail(email) != null) "${xwikiUser.username}@imported.local" else email
                        val finalEmail = if (userService.findByEmail(safeEmail) != null) "${xwikiUser.username}.${System.currentTimeMillis() % 10000}@imported.local" else safeEmail

                        val displayName = listOfNotNull(xwikiUser.firstName, xwikiUser.lastName).joinToString(" ").ifEmpty { xwikiUser.username }
                        userService.create(User(
                            username = xwikiUser.username,
                            email = finalEmail,
                            firstName = xwikiUser.firstName,
                            lastName = xwikiUser.lastName,
                            displayName = displayName,
                            status = UserStatus.ACTIVE,
                            externalId = "xwiki:${xwikiUser.username}",
                        ))
                        usersCreated++
                    }
                } catch (e: Exception) {
                    logger.warn("[AutoSync] User sync error for ${xwikiUser.username}: ${e.message}")
                }
            }

            // Phase 2: Sync groups
            val xwikiGroups = client.getGroups()
            xwikiGroups.forEach { xwikiGroup ->
                try {
                    if (groupService.findByName(xwikiGroup.name) == null) {
                        groupService.create(Group(name = xwikiGroup.name, description = "Imported from xWiki", type = GroupType.STATIC))
                        groupsCreated++
                    }
                } catch (e: Exception) {
                    logger.warn("[AutoSync] Group sync error for ${xwikiGroup.name}: ${e.message}")
                }
            }

            // Phase 3: Sync memberships
            xwikiGroups.forEach { xwikiGroup ->
                try {
                    val group = groupService.findByName(xwikiGroup.name) ?: return@forEach
                    val memberUsernames = client.getGroupMembers(xwikiGroup.name)
                    val existingMembers = groupService.getMembers(group.id!!)

                    memberUsernames.forEach { memberUsername ->
                        val user = userService.findByUsername(memberUsername)
                        if (user != null && user.id!! !in existingMembers) {
                            try { groupService.addMember(group.id!!, user.id!!); membersLinked++ } catch (_: Exception) {}
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("[AutoSync] Membership sync error for ${xwikiGroup.name}: ${e.message}")
                }
            }

            // Update connector last sync timestamp
            connectorService.update(connector.id!!) { c ->
                c.lastSyncAt = Instant.now()
                c
            }

            val duration = System.currentTimeMillis() - startTime
            logger.info("[AutoSync] xWiki sync complete in ${duration}ms: users(+$usersCreated, ~$usersUpdated), groups(+$groupsCreated), members(+$membersLinked)")

        } catch (e: Exception) {
            logger.error("[AutoSync] xWiki sync failed for ${connector.name}: ${e.message}")
        }
    }

    private fun buildXWikiClient(connector: org.opencrowd.core.entity.Connector): XWikiClient? {
        return try {
            val configMap = objectMapper.readValue<Map<String, String>>(connector.config)
            val baseUrl = configMap["baseUrl"] ?: return null
            val username = configMap["username"] ?: return null
            val password = configMap["password"] ?: return null
            val client = XWikiClient(baseUrl.trimEnd('/'), username, password)
            if (client.testConnection()) client else {
                logger.warn("[AutoSync] Connection test failed for ${connector.name}")
                null
            }
        } catch (e: Exception) {
            logger.error("[AutoSync] Failed to build client for ${connector.name}: ${e.message}")
            null
        }
    }

    companion object {
        private val SYSTEM_USERS = setOf("XWikiGuest", "superadmin", "XWikiRobot", "XWikiDefaultSkin")
    }
}
