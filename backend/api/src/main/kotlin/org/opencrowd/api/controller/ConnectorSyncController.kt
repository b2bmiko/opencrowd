package org.opencrowd.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.runBlocking
import org.opencrowd.connectors.sdk.AuthConfig
import org.opencrowd.connectors.sdk.ConnectorConfig
import org.opencrowd.connectors.sdk.ConnectorRegistry
import org.opencrowd.connectors.sdk.ConnectorResult
import org.opencrowd.connectors.sdk.SyncOptions
import org.opencrowd.connectors.xwiki.XWikiClient
import org.opencrowd.core.entity.Group
import org.opencrowd.core.entity.GroupType
import org.opencrowd.core.entity.User
import org.opencrowd.core.entity.UserStatus
import org.opencrowd.core.multitenancy.TenantContext
import org.opencrowd.core.service.ConnectorService
import org.opencrowd.core.service.GroupService
import org.opencrowd.core.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/connectors")
@Tag(name = "Connector Sync", description = "Synchronize data from connected applications")
class ConnectorSyncController(
    private val connectorService: ConnectorService,
    private val connectorRegistry: ConnectorRegistry,
    private val userService: UserService,
    private val groupService: GroupService,
) {

    private val logger = LoggerFactory.getLogger(ConnectorSyncController::class.java)

    @PostMapping("/{id}/import-users")
    @Operation(summary = "Import users", description = "Imports users from connected application into OpenCrowd")
    @PreAuthorize("hasRole('manage_connectors')")
    fun importUsers(
        @PathVariable id: UUID,
        @RequestBody body: Map<String, String>
    ): ResponseEntity<Map<String, Any>> {
        ensureTenantContext()
        val dbConnector = connectorService.findById(id)
            ?: return ResponseEntity.notFound().build()

        val xwikiClient = buildXWikiClient(body)
            ?: return ResponseEntity.ok(mapOf("success" to false, "error" to "Connection failed"))

        val xwikiUsers = xwikiClient.getUsers()
        logger.info("Found ${xwikiUsers.size} users in xWiki to import")

        var created = 0
        var updated = 0
        var skipped = 0
        var errors = 0
        val importedUsers = mutableListOf<String>()
        val failedUsers = mutableListOf<String>()

        xwikiUsers.forEach { xwikiUser ->
            try {
                // Skip system/bot users
                if (xwikiUser.username in listOf("XWikiGuest", "superadmin", "XWikiRobot", "XWikiDefaultSkin")) {
                    skipped++
                    return@forEach
                }

                val existing = userService.findByUsername(xwikiUser.username)
                if (existing != null) {
                    // Update existing user with fresh details from xWiki
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
                        updated++
                    } else {
                        skipped++
                    }
                    return@forEach
                }

                // Check email uniqueness before creating
                val email = xwikiUser.email?.takeIf { it.isNotBlank() } ?: "${xwikiUser.username}@imported.local"
                val emailExists = userService.findByEmail(email)
                val finalEmail = if (emailExists != null) "${xwikiUser.username}@imported.local" else email
                // If even the fallback email conflicts (shouldn't happen), make it truly unique
                val safeEmail = if (userService.findByEmail(finalEmail) != null) "${xwikiUser.username}.${System.currentTimeMillis() % 10000}@imported.local" else finalEmail

                val displayName = listOfNotNull(xwikiUser.firstName, xwikiUser.lastName).joinToString(" ").ifEmpty { xwikiUser.username }
                val user = User(
                    username = xwikiUser.username,
                    email = safeEmail,
                    firstName = xwikiUser.firstName,
                    lastName = xwikiUser.lastName,
                    displayName = displayName,
                    status = UserStatus.ACTIVE,
                    externalId = "xwiki:${xwikiUser.username}",
                )
                userService.create(user)
                created++
                importedUsers.add(xwikiUser.username)
            } catch (e: Exception) {
                logger.warn("Failed to import user ${xwikiUser.username}: ${e.message}")
                failedUsers.add("${xwikiUser.username}: ${e.message?.take(80)}")
                errors++
            }
        }

        logger.info("xWiki user import complete: created=$created, updated=$updated, skipped=$skipped, errors=$errors")

        return ResponseEntity.ok(mapOf(
            "success" to true,
            "created" to created,
            "updated" to updated,
            "skipped" to skipped,
            "errors" to errors,
            "total" to xwikiUsers.size,
            "importedUsers" to importedUsers,
            "failedUsers" to failedUsers,
        ))
    }

    @PostMapping("/{id}/provision-user")
    @Operation(summary = "Provision user to app", description = "Creates a user in the connected application from OpenCrowd")
    @PreAuthorize("hasRole('manage_connectors')")
    fun provisionUser(
        @PathVariable id: UUID,
        @RequestBody body: Map<String, String>
    ): ResponseEntity<Map<String, Any>> {
        val dbConnector = connectorService.findById(id)
            ?: return ResponseEntity.notFound().build()

        val baseUrl = body["baseUrl"] ?: throw IllegalArgumentException("Missing baseUrl")
        val connUsername = body["username"] ?: throw IllegalArgumentException("Missing username")
        val connPassword = body["password"] ?: throw IllegalArgumentException("Missing password")
        val targetUserId = body["userId"] ?: throw IllegalArgumentException("Missing userId")

        // Get the user from OpenCrowd
        val user = userService.findById(UUID.fromString(targetUserId))
            ?: return ResponseEntity.ok(mapOf("success" to false, "error" to "User not found in OpenCrowd"))

        val xwikiClient = XWikiClient(baseUrl.trimEnd('/'), connUsername, connPassword)

        if (!xwikiClient.testConnection()) {
            return ResponseEntity.ok(mapOf("success" to false, "error" to "Connection failed"))
        }

        val created = xwikiClient.createUser(
            username = user.username,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            password = null, // Don't set password — user will reset via xWiki
        )

        return if (created) {
            ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "User '${user.username}' provisioned to xWiki",
            ))
        } else {
            ResponseEntity.ok(mapOf(
                "success" to false,
                "error" to "Failed to create user in xWiki",
            ))
        }
    }

    @PostMapping("/{id}/import-groups")
    @Operation(summary = "Import groups", description = "Imports groups from connected application into OpenCrowd")
    @PreAuthorize("hasRole('manage_connectors')")
    fun importGroups(
        @PathVariable id: UUID,
        @RequestBody body: Map<String, String>
    ): ResponseEntity<Map<String, Any>> {
        ensureTenantContext()
        val dbConnector = connectorService.findById(id)
            ?: return ResponseEntity.notFound().build()

        val baseUrl = body["baseUrl"] ?: throw IllegalArgumentException("Missing baseUrl")
        val username = body["username"] ?: throw IllegalArgumentException("Missing username")
        val password = body["password"] ?: throw IllegalArgumentException("Missing password")

        val xwikiClient = XWikiClient(baseUrl.trimEnd('/'), username, password)

        if (!xwikiClient.testConnection()) {
            return ResponseEntity.ok(mapOf("success" to false, "error" to "Connection failed"))
        }

        // Fetch groups from xWiki
        val xwikiGroups = xwikiClient.getGroups()
        logger.info("Found ${xwikiGroups.size} groups in xWiki to import")

        var created = 0
        var skipped = 0
        var errors = 0
        val importedGroups = mutableListOf<String>()

        xwikiGroups.forEach { xwikiGroup ->
            try {
                val existing = groupService.findByName(xwikiGroup.name)
                if (existing != null) {
                    skipped++
                    return@forEach
                }

                val group = Group(
                    name = xwikiGroup.name,
                    description = "Imported from xWiki (${xwikiGroup.fullName})",
                    type = GroupType.STATIC,
                )
                groupService.create(group)
                created++
                importedGroups.add(xwikiGroup.name)
            } catch (e: Exception) {
                logger.warn("Failed to import group ${xwikiGroup.name}: ${e.message}")
                errors++
            }
        }

        // Import group members
        var membersLinked = 0
        xwikiGroups.forEach { xwikiGroup ->
            try {
                val group = groupService.findByName(xwikiGroup.name) ?: return@forEach
                val memberUsernames = xwikiClient.getGroupMembers(xwikiGroup.name)

                memberUsernames.forEach { memberUsername ->
                    val user = userService.findByUsername(memberUsername)
                    if (user != null && group.id != null) {
                        try {
                            groupService.addMember(group.id!!, user.id!!)
                            membersLinked++
                        } catch (_: Exception) {
                            // Already a member or other constraint — skip
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Failed to import members for group ${xwikiGroup.name}: ${e.message}")
            }
        }

        logger.info("xWiki group import complete: created=$created, skipped=$skipped, errors=$errors, membersLinked=$membersLinked")

        return ResponseEntity.ok(mapOf(
            "success" to true,
            "created" to created,
            "skipped" to skipped,
            "errors" to errors,
            "membersLinked" to membersLinked,
            "total" to xwikiGroups.size,
            "importedGroups" to importedGroups,
        ))
    }

    @PostMapping("/{id}/sync-all")
    @Operation(summary = "Full sync", description = "Imports all users, groups, and memberships from connected application in one operation")
    @PreAuthorize("hasRole('manage_connectors')")
    fun syncAll(
        @PathVariable id: UUID,
        @RequestBody body: Map<String, String>
    ): ResponseEntity<Map<String, Any>> {
        ensureTenantContext()
        val dbConnector = connectorService.findById(id)
            ?: return ResponseEntity.notFound().build()

        val xwikiClient = buildXWikiClient(body)
            ?: return ResponseEntity.ok(mapOf("success" to false, "error" to "Connection failed"))
        logger.info("[SyncAll] Starting full sync from xWiki...")

        // === Phase 1: Import users ===
        val xwikiUsers = xwikiClient.getUsers()
        logger.info("[SyncAll] Found ${xwikiUsers.size} users in xWiki")

        var usersCreated = 0
        var usersUpdated = 0
        var usersErrors = 0
        val failedUsers = mutableListOf<String>()

        xwikiUsers.forEach { xwikiUser ->
            try {
                // Skip system/bot users
                if (xwikiUser.username in listOf("XWikiGuest", "superadmin", "XWikiRobot", "XWikiDefaultSkin")) {
                    return@forEach
                }

                val existing = userService.findByUsername(xwikiUser.username)
                if (existing != null) {
                    // Update with fresh data
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
                    val emailExists = userService.findByEmail(email)
                    val finalEmail = if (emailExists != null) "${xwikiUser.username}@imported.local" else email
                    val safeEmail = if (userService.findByEmail(finalEmail) != null) "${xwikiUser.username}.${System.currentTimeMillis() % 10000}@imported.local" else finalEmail

                    val displayName = listOfNotNull(xwikiUser.firstName, xwikiUser.lastName).joinToString(" ").ifEmpty { xwikiUser.username }
                    val user = User(
                        username = xwikiUser.username,
                        email = safeEmail,
                        firstName = xwikiUser.firstName,
                        lastName = xwikiUser.lastName,
                        displayName = displayName,
                        status = UserStatus.ACTIVE,
                        externalId = "xwiki:${xwikiUser.username}",
                    )
                    userService.create(user)
                    usersCreated++
                }
            } catch (e: Exception) {
                logger.warn("[SyncAll] User import error for ${xwikiUser.username}: ${e.message}")
                failedUsers.add("${xwikiUser.username}: ${e.message?.take(80)}")
                usersErrors++
            }
        }

        // === Phase 2: Import groups ===
        val xwikiGroups = xwikiClient.getGroups()
        logger.info("[SyncAll] Found ${xwikiGroups.size} groups in xWiki")

        var groupsCreated = 0
        var groupsSkipped = 0

        xwikiGroups.forEach { xwikiGroup ->
            try {
                val existing = groupService.findByName(xwikiGroup.name)
                if (existing != null) {
                    groupsSkipped++
                    return@forEach
                }
                val group = Group(
                    name = xwikiGroup.name,
                    description = "Imported from xWiki",
                    type = GroupType.STATIC,
                )
                groupService.create(group)
                groupsCreated++
            } catch (e: Exception) {
                logger.warn("[SyncAll] Group import error for ${xwikiGroup.name}: ${e.message}")
            }
        }

        // === Phase 3: Sync group memberships ===
        var membersLinked = 0
        var membersSkipped = 0

        xwikiGroups.forEach { xwikiGroup ->
            try {
                val group = groupService.findByName(xwikiGroup.name) ?: return@forEach
                val memberUsernames = xwikiClient.getGroupMembers(xwikiGroup.name)
                val existingMembers = groupService.getMembers(group.id!!)

                memberUsernames.forEach { memberUsername ->
                    val user = userService.findByUsername(memberUsername)
                    if (user != null && group.id != null && user.id!! !in existingMembers) {
                        try {
                            groupService.addMember(group.id!!, user.id!!)
                            membersLinked++
                        } catch (_: Exception) {
                            membersSkipped++
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("[SyncAll] Membership sync error for ${xwikiGroup.name}: ${e.message}")
            }
        }

        logger.info("[SyncAll] Complete: users(created=$usersCreated, updated=$usersUpdated), groups(created=$groupsCreated), members(linked=$membersLinked)")

        return ResponseEntity.ok(mapOf(
            "success" to true,
            "users" to mapOf(
                "total" to xwikiUsers.size,
                "created" to usersCreated,
                "updated" to usersUpdated,
                "errors" to usersErrors,
                "failed" to failedUsers,
            ),
            "groups" to mapOf(
                "total" to xwikiGroups.size,
                "created" to groupsCreated,
                "skipped" to groupsSkipped,
            ),
            "memberships" to mapOf(
                "linked" to membersLinked,
                "skipped" to membersSkipped,
            ),
        ))
    }

    @PostMapping("/{id}/resync")
    @Operation(summary = "Re-sync using stored credentials", description = "Syncs users, groups, and memberships using the connector's saved credentials")
    @PreAuthorize("hasRole('manage_connectors')")
    fun resync(@PathVariable id: UUID): ResponseEntity<Map<String, Any>> {
        ensureTenantContext()
        val dbConnector = connectorService.findById(id)
            ?: return ResponseEntity.notFound().build()

        if (dbConnector.config == "{}") {
            return ResponseEntity.ok(mapOf("success" to false, "error" to "No credentials saved. Use Test Connection first."))
        }

        val xwikiClient = buildXWikiClientFromConfig(dbConnector.config)
            ?: return ResponseEntity.ok(mapOf("success" to false, "error" to "Connection failed — stored credentials may be invalid"))

        logger.info("[Resync] Starting full sync from stored credentials for connector: ${dbConnector.name}")

        // === Phase 1: Import users ===
        val xwikiUsers = xwikiClient.getUsers()
        var usersCreated = 0
        var usersUpdated = 0
        var usersErrors = 0
        val failedUsers = mutableListOf<String>()

        xwikiUsers.forEach { xwikiUser ->
            try {
                if (xwikiUser.username in listOf("XWikiGuest", "superadmin", "XWikiRobot", "XWikiDefaultSkin")) return@forEach

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
                    val emailExists = userService.findByEmail(email)
                    val finalEmail = if (emailExists != null) "${xwikiUser.username}@imported.local" else email
                    val safeEmail = if (userService.findByEmail(finalEmail) != null) "${xwikiUser.username}.${System.currentTimeMillis() % 10000}@imported.local" else finalEmail

                    val displayName = listOfNotNull(xwikiUser.firstName, xwikiUser.lastName).joinToString(" ").ifEmpty { xwikiUser.username }
                    val user = User(
                        username = xwikiUser.username,
                        email = safeEmail,
                        firstName = xwikiUser.firstName,
                        lastName = xwikiUser.lastName,
                        displayName = displayName,
                        status = UserStatus.ACTIVE,
                        externalId = "xwiki:${xwikiUser.username}",
                    )
                    userService.create(user)
                    usersCreated++
                }
            } catch (e: Exception) {
                logger.warn("[Resync] User import error for ${xwikiUser.username}: ${e.message}")
                failedUsers.add("${xwikiUser.username}: ${e.message?.take(80)}")
                usersErrors++
            }
        }

        // === Phase 2: Import groups ===
        val xwikiGroups = xwikiClient.getGroups()
        var groupsCreated = 0
        var groupsSkipped = 0

        xwikiGroups.forEach { xwikiGroup ->
            try {
                val existing = groupService.findByName(xwikiGroup.name)
                if (existing != null) { groupsSkipped++; return@forEach }
                val group = Group(name = xwikiGroup.name, description = "Imported from xWiki", type = GroupType.STATIC)
                groupService.create(group)
                groupsCreated++
            } catch (e: Exception) {
                logger.warn("[Resync] Group import error: ${e.message}")
            }
        }

        // === Phase 3: Sync memberships ===
        var membersLinked = 0

        xwikiGroups.forEach { xwikiGroup ->
            try {
                val group = groupService.findByName(xwikiGroup.name) ?: return@forEach
                val memberUsernames = xwikiClient.getGroupMembers(xwikiGroup.name)
                val existingMembers = groupService.getMembers(group.id!!)

                memberUsernames.forEach { memberUsername ->
                    val user = userService.findByUsername(memberUsername)
                    if (user != null && user.id!! !in existingMembers) {
                        try { groupService.addMember(group.id!!, user.id!!); membersLinked++ } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                logger.warn("[Resync] Membership sync error: ${e.message}")
            }
        }

        logger.info("[Resync] Complete: users(created=$usersCreated, updated=$usersUpdated, errors=$usersErrors), groups(created=$groupsCreated), members(linked=$membersLinked)")

        return ResponseEntity.ok(mapOf(
            "success" to true,
            "users" to mapOf("total" to xwikiUsers.size, "created" to usersCreated, "updated" to usersUpdated, "errors" to usersErrors, "failed" to failedUsers),
            "groups" to mapOf("total" to xwikiGroups.size, "created" to groupsCreated, "skipped" to groupsSkipped),
            "memberships" to mapOf("linked" to membersLinked),
        ))
    }

    // --- Helpers ---

    private fun buildXWikiClient(body: Map<String, String>): XWikiClient? {
        val baseUrl = body["baseUrl"] ?: throw IllegalArgumentException("Missing baseUrl")
        val username = body["username"] ?: throw IllegalArgumentException("Missing username")
        val password = body["password"] ?: throw IllegalArgumentException("Missing password")

        val client = XWikiClient(baseUrl.trimEnd('/'), username, password)
        return if (client.testConnection()) client else null
    }

    private fun buildXWikiClientFromConfig(config: String): XWikiClient? {
        return try {
            val mapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
            val configMap: Map<String, String> = mapper.readValue(config, object : com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {})
            val baseUrl = configMap["baseUrl"] ?: return null
            val username = configMap["username"] ?: return null
            val password = configMap["password"] ?: return null
            val client = XWikiClient(baseUrl.trimEnd('/'), username, password)
            if (client.testConnection()) client else null
        } catch (e: Exception) {
            logger.error("Failed to build xWiki client from stored config: ${e.message}")
            null
        }
    }

    /**
     * Ensures the TenantContext is set for the current thread.
     * This is critical for multi-tenancy: without it, Hibernate uses the public schema
     * which has no users/groups tables.
     * In dev mode (no JWT), falls back to "acme" tenant.
     */
    private fun ensureTenantContext() {
        if (TenantContext.getTenantId() == null) {
            TenantContext.setTenantId("acme")
            logger.debug("TenantContext was null, set to 'acme' (dev fallback)")
        }
    }
}
