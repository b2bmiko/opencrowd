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
        val dbConnector = connectorService.findById(id)
            ?: return ResponseEntity.notFound().build()

        val baseUrl = body["baseUrl"] ?: throw IllegalArgumentException("Missing baseUrl")
        val username = body["username"] ?: throw IllegalArgumentException("Missing username")
        val password = body["password"] ?: throw IllegalArgumentException("Missing password")

        // Create xWiki client directly for import
        val xwikiClient = XWikiClient(baseUrl.trimEnd('/'), username, password)

        if (!xwikiClient.testConnection()) {
            return ResponseEntity.ok(mapOf("success" to false, "error" to "Connection failed"))
        }

        // Fetch users from xWiki
        val xwikiUsers = xwikiClient.getUsers()
        logger.info("Found ${xwikiUsers.size} users in xWiki to import")

        var created = 0
        var skipped = 0
        var errors = 0
        val importedUsers = mutableListOf<String>()

        xwikiUsers.forEach { xwikiUser ->
            try {
                // Check if user already exists
                val existing = userService.findByUsername(xwikiUser.username)
                if (existing != null) {
                    skipped++
                    return@forEach
                }

                // Create user in OpenCrowd
                val user = User(
                    username = xwikiUser.username,
                    email = xwikiUser.email ?: "${xwikiUser.username}@imported.local",
                    firstName = xwikiUser.firstName,
                    lastName = xwikiUser.lastName,
                    displayName = xwikiUser.username,
                    status = UserStatus.ACTIVE,
                    externalId = "xwiki:${xwikiUser.id}",
                )
                userService.create(user)
                created++
                importedUsers.add(xwikiUser.username)
            } catch (e: Exception) {
                logger.warn("Failed to import user ${xwikiUser.username}: ${e.message}")
                errors++
            }
        }

        logger.info("xWiki user import complete: created=$created, skipped=$skipped, errors=$errors")

        return ResponseEntity.ok(mapOf(
            "success" to true,
            "created" to created,
            "skipped" to skipped,
            "errors" to errors,
            "total" to xwikiUsers.size,
            "importedUsers" to importedUsers,
        ))
    }

    @PostMapping("/{id}/import-groups")
    @Operation(summary = "Import groups", description = "Imports groups from connected application into OpenCrowd")
    @PreAuthorize("hasRole('manage_connectors')")
    fun importGroups(
        @PathVariable id: UUID,
        @RequestBody body: Map<String, String>
    ): ResponseEntity<Map<String, Any>> {
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

        logger.info("xWiki group import complete: created=$created, skipped=$skipped, errors=$errors")

        return ResponseEntity.ok(mapOf(
            "success" to true,
            "created" to created,
            "skipped" to skipped,
            "errors" to errors,
            "total" to xwikiGroups.size,
            "importedGroups" to importedGroups,
        ))
    }
}
