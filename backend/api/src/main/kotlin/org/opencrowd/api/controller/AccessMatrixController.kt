package org.opencrowd.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.opencrowd.connectors.xwiki.XWikiClient
import org.opencrowd.core.entity.AccessEntry
import org.opencrowd.core.entity.PrincipalType
import org.opencrowd.core.repository.AccessEntryRepository
import org.opencrowd.core.service.ConnectorService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/v1/access-matrix")
@Tag(name = "Access Matrix", description = "Unified cross-application permission view")
class AccessMatrixController(
    private val accessEntryRepository: AccessEntryRepository,
    private val connectorService: ConnectorService,
    private val eventPublisher: org.opencrowd.core.event.DomainEventPublisher,
) {

    private val logger = LoggerFactory.getLogger(AccessMatrixController::class.java)

    @GetMapping
    @Operation(summary = "Get access matrix", description = "Returns all access entries across connected apps")
    fun getAccessMatrix(
        @RequestParam(required = false) application: String?,
        @RequestParam(required = false) principalName: String?,
        @RequestParam(required = false) resourceName: String?,
    ): ResponseEntity<Map<String, Any>> {
        val entries = when {
            application != null -> accessEntryRepository.findByApplication(application)
            principalName != null -> accessEntryRepository.findByPrincipalName(principalName)
            resourceName != null -> accessEntryRepository.findByResourceTypeAndResourceName("space", resourceName)
            else -> accessEntryRepository.findAll()
        }

        val response = entries.map { entry ->
            mapOf(
                "id" to entry.id,
                "principalType" to entry.principalType.name,
                "principalName" to entry.principalName,
                "application" to entry.application,
                "resourceType" to entry.resourceType,
                "resourceName" to entry.resourceName,
                "permission" to entry.permission,
                "allow" to entry.allow,
                "source" to entry.source,
                "syncedAt" to entry.syncedAt.toString(),
            )
        }

        return ResponseEntity.ok(mapOf(
            "entries" to response,
            "count" to response.size,
        ))
    }

    @PostMapping("/toggle")
    @Operation(summary = "Toggle permission", description = "Grant or revoke a permission for a principal")
    @PreAuthorize("hasRole('manage_connectors')")
    @org.springframework.transaction.annotation.Transactional
    fun togglePermission(@RequestBody body: Map<String, String>): ResponseEntity<Map<String, Any>> {
        val principalName = body["principalName"] ?: throw IllegalArgumentException("Missing principalName")
        val principalType = body["principalType"] ?: throw IllegalArgumentException("Missing principalType")
        val permission = body["permission"] ?: throw IllegalArgumentException("Missing permission")
        val application = body["application"] ?: "xwiki"
        val resourceName = body["resourceName"] ?: "(global)"
        val resourceType = body["resourceType"] ?: "wiki"
        val action = body["action"] ?: "grant" // "grant" or "revoke"

        // Also write back to the connected app
        val writeBackResult = writeBackToApp(application, principalName, principalType, permission, resourceName, action)

        if (action == "revoke") {
            // Remove from DB
            val entries = accessEntryRepository.findByPrincipalName(principalName)
                .filter { it.permission == permission && it.application == application && it.resourceName == resourceName }
            entries.forEach { accessEntryRepository.delete(it) }

            eventPublisher.publish(org.opencrowd.core.event.PermissionPushed(
                tenantId = org.opencrowd.core.multitenancy.TenantContext.getTenantId() ?: "acme",
                actorId = null, correlationId = java.util.UUID.randomUUID().toString(),
                principalName = principalName, principalType = principalType,
                permission = permission, resourceName = resourceName,
                application = application, action = "revoked",
            ))

            return ResponseEntity.ok(mapOf(
                "success" to true,
                "action" to "revoked",
                "message" to "Permission '$permission' revoked from '$principalName'",
                "writeBack" to writeBackResult,
            ))
        } else {
            // Add to DB
            val entry = AccessEntry(
                principalType = PrincipalType.valueOf(principalType),
                principalName = principalName,
                application = application,
                resourceType = resourceType,
                resourceName = resourceName,
                permission = permission,
                allow = true,
                source = "manual",
                syncedAt = java.time.Instant.now(),
            )
            accessEntryRepository.save(entry)

            eventPublisher.publish(org.opencrowd.core.event.PermissionPushed(
                tenantId = org.opencrowd.core.multitenancy.TenantContext.getTenantId() ?: "acme",
                actorId = null, correlationId = java.util.UUID.randomUUID().toString(),
                principalName = principalName, principalType = principalType,
                permission = permission, resourceName = resourceName,
                application = application, action = "granted",
            ))

            return ResponseEntity.ok(mapOf(
                "success" to true,
                "action" to "granted",
                "message" to "Permission '$permission' granted to '$principalName'",
                "writeBack" to writeBackResult,
            ))
        }
    }

    private fun writeBackToApp(application: String, principalName: String, principalType: String, permission: String, resourceName: String, action: String): String {
        return when (application) {
            "xwiki" -> writeBackToXWiki(principalName, principalType, permission, resourceName, action)
            "openproject" -> writeBackToOpenProject(principalName, principalType, permission, resourceName, action)
            else -> "write-back not implemented for $application"
        }
    }

    private fun writeBackToXWiki(principalName: String, principalType: String, permission: String, resourceName: String, action: String): String {
        val connectors = connectorService.findAll().filter { it.connectorType == "xwiki" && it.config != "{}" }
        if (connectors.isEmpty()) return "no xwiki connector with credentials found"

        val connector = connectors.first()
        return try {
            val configMap = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
                .readValue<Map<String, String>>(connector.config)
            val baseUrl = configMap["baseUrl"] ?: return "missing baseUrl in connector config"
            val username = configMap["username"] ?: return "missing username"
            val password = configMap["password"] ?: return "missing password"

            val client = org.opencrowd.connectors.xwiki.XWikiClient(baseUrl.trimEnd('/'), username, password)

            if (action == "grant") {
                val spaceName = if (resourceName == "(global)") "(global)" else resourceName
                val isGroup = principalType == "GROUP"
                val success = client.addRight(spaceName, principalName, isGroup, listOf(permission))
                if (success) "written to xWiki" else "xWiki write failed"
            } else {
                val spaceName = if (resourceName == "(global)") "(global)" else resourceName
                val isGroup = principalType == "GROUP"
                val success = client.removeRight(spaceName, principalName, isGroup, listOf(permission))
                if (success) "revoked from xWiki" else "xWiki revoke failed"
            }
        } catch (e: Exception) {
            "write-back error: ${e.message}"
        }
    }

    private fun writeBackToOpenProject(principalName: String, principalType: String, permission: String, resourceName: String, action: String): String {
        val connectors = connectorService.findAll().filter { it.connectorType == "openproject" && it.config != "{}" }
        if (connectors.isEmpty()) return "no openproject connector with credentials found"

        val connector = connectors.first()
        return try {
            val configMap = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
                .readValue<Map<String, String>>(connector.config)
            val baseUrl = configMap["baseUrl"] ?: return "missing baseUrl"
            val apiKey = configMap["password"] ?: configMap["apiKey"] ?: return "missing apiKey"

            val client = org.opencrowd.connectors.openproject.OpenProjectClient(baseUrl.trimEnd('/'), apiKey)

            if (action == "grant") {
                // Find the user ID and role ID in OpenProject
                val users = client.getUsers()
                val opUser = users.find { 
                    it.login == principalName || 
                    "${it.firstName} ${it.lastName}".trim() == principalName ||
                    it.firstName == principalName
                } ?: return "user '$principalName' not found in OpenProject"

                val roles = client.getRoles()
                val role = roles.find { it.name.equals(permission, ignoreCase = true) }
                    ?: return "role '$permission' not found in OpenProject"

                // Find the project — use resourceName or first project the user is already in
                val projects = client.getProjects()
                val project = if (resourceName == "(global)") {
                    // Try to find a project the user already has access to
                    val existingMemberships = client.getMemberships()
                    val userMembership = existingMemberships.find { it.principalId == opUser.id }
                    if (userMembership != null) {
                        projects.find { it.name == userMembership.projectName }
                    } else {
                        projects.firstOrNull()
                    }
                } else {
                    projects.find { it.name == resourceName || it.identifier == resourceName }
                } ?: return "no suitable project found in OpenProject"

                val success = client.addMembership(project.id, opUser.id, role.id)
                if (success) "written to OpenProject (project: ${project.name})" else "OpenProject write failed"
            } else {
                // Revoke: find and remove the membership
                val memberships = client.getMemberships()
                val match = memberships.find {
                    it.principalName == principalName && it.roles.any { r -> r.equals(permission, ignoreCase = true) }
                            && (resourceName == "(global)" || it.projectName == resourceName)
                }
                if (match != null) {
                    val success = client.removeMembership(match.id)
                    if (success) "revoked from OpenProject" else "OpenProject revoke failed"
                } else {
                    "membership not found in OpenProject"
                }
            }
        } catch (e: Exception) {
            "write-back error: ${e.message}"
        }
    }

    @PostMapping("/push-to-apps")
    @Operation(summary = "Push permissions to apps", description = "Writes all granted permissions from OpenCrowd to connected applications")
    @PreAuthorize("hasRole('manage_connectors')")
    fun pushToApps(): ResponseEntity<Map<String, Any>> {
        // Push ALL granted permissions to xWiki (regardless of source)
        val entries = accessEntryRepository.findAll().filter { it.allow && it.permission != "(none)" }
        if (entries.isEmpty()) {
            return ResponseEntity.ok(mapOf("success" to true, "pushed" to 0, "message" to "No permissions to push"))
        }

        val connectors = connectorService.findAll().filter { it.connectorType == "xwiki" && it.config != "{}" }
        if (connectors.isEmpty()) {
            return ResponseEntity.ok(mapOf("success" to false, "error" to "No xWiki connector with saved credentials"))
        }

        val connector = connectors.first()
        val configMap = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
            .readValue<Map<String, String>>(connector.config)
        val baseUrl = configMap["baseUrl"] ?: return ResponseEntity.ok(mapOf("success" to false, "error" to "Missing baseUrl"))
        val username = configMap["username"] ?: return ResponseEntity.ok(mapOf("success" to false, "error" to "Missing username"))
        val password = configMap["password"] ?: return ResponseEntity.ok(mapOf("success" to false, "error" to "Missing password"))

        val client = org.opencrowd.connectors.xwiki.XWikiClient(baseUrl.trimEnd('/'), username, password)

        var pushed = 0
        var failed = 0
        val results = mutableListOf<String>()

        entries.forEach { entry ->
            try {
                val spaceName = if (entry.resourceName == "(global)") "(global)" else entry.resourceName
                val isGroup = entry.principalType == org.opencrowd.core.entity.PrincipalType.GROUP
                val success = client.addRight(spaceName, entry.principalName, isGroup, listOf(entry.permission))
                if (success) {
                    pushed++
                } else {
                    failed++
                    results.add("${entry.principalName}/${entry.permission}: write failed")
                }
            } catch (e: Exception) {
                failed++
                results.add("${entry.principalName}/${entry.permission}: ${e.message?.take(60)}")
            }
        }

        logger.info("Push to apps complete: pushed=$pushed, failed=$failed")

        return ResponseEntity.ok(mapOf(
            "success" to true,
            "pushed" to pushed,
            "failed" to failed,
            "details" to results,
        ))
    }

    @PostMapping("/sync-rights")
    @Operation(summary = "Sync rights from xWiki", description = "Fetches permissions from xWiki and stores them in the Access Matrix")
    @PreAuthorize("hasRole('manage_connectors')")
    @org.springframework.transaction.annotation.Transactional
    fun syncRights(@RequestBody body: Map<String, String>): ResponseEntity<Map<String, Any>> {
        val baseUrl = body["baseUrl"] ?: throw IllegalArgumentException("Missing baseUrl")
        val username = body["username"] ?: throw IllegalArgumentException("Missing username")
        val password = body["password"] ?: throw IllegalArgumentException("Missing password")

        val xwikiClient = XWikiClient(baseUrl.trimEnd('/'), username, password)

        if (!xwikiClient.testConnection()) {
            return ResponseEntity.ok(mapOf("success" to false, "error" to "Connection failed"))
        }

        // Clear existing synced entries for xwiki
        accessEntryRepository.deleteByApplicationAndSource("xwiki", "synced")

        var entriesCreated = 0

        // 1. Fetch global rights
        val globalRights = xwikiClient.getGlobalRights()
        globalRights.forEach { right ->
            right.levels.forEach { level ->
                // Create entries for users
                right.users.forEach { user ->
                    accessEntryRepository.save(AccessEntry(
                        principalType = PrincipalType.USER,
                        principalName = user,
                        application = "xwiki",
                        resourceType = "wiki",
                        resourceName = "(global)",
                        permission = level,
                        allow = right.allow,
                        source = "synced",
                        syncedAt = Instant.now(),
                    ))
                    entriesCreated++
                }
                // Create entries for groups
                right.groups.forEach { group ->
                    accessEntryRepository.save(AccessEntry(
                        principalType = PrincipalType.GROUP,
                        principalName = group,
                        application = "xwiki",
                        resourceType = "wiki",
                        resourceName = "(global)",
                        permission = level,
                        allow = right.allow,
                        source = "synced",
                        syncedAt = Instant.now(),
                    ))
                    entriesCreated++
                }
            }
        }

        // 2. Fetch space-level rights for key spaces
        val spaces = xwikiClient.getSpaces()
        val relevantSpaces = spaces.filter { space ->
            // Skip system/internal spaces but keep all user-created spaces (including nested ones)
            !space.name.startsWith("Help") &&
            !space.name.startsWith("Menu") &&
            !space.name.startsWith("Sandbox") &&
            !space.name.startsWith("XWikiCrowd") &&
            space.name != "XWiki" &&
            space.name != "Main"
        }

        relevantSpaces.forEach { space ->
            try {
                val spaceRights = xwikiClient.getSpaceRights(space.name)
                spaceRights.forEach { right ->
                    right.levels.forEach { level ->
                        right.users.forEach { user ->
                            accessEntryRepository.save(AccessEntry(
                                principalType = PrincipalType.USER,
                                principalName = user,
                                application = "xwiki",
                                resourceType = "space",
                                resourceName = space.name,
                                permission = level,
                                allow = right.allow,
                                source = "synced",
                                syncedAt = Instant.now(),
                            ))
                            entriesCreated++
                        }
                        right.groups.forEach { group ->
                            accessEntryRepository.save(AccessEntry(
                                principalType = PrincipalType.GROUP,
                                principalName = group,
                                application = "xwiki",
                                resourceType = "space",
                                resourceName = space.name,
                                permission = level,
                                allow = right.allow,
                                source = "synced",
                                syncedAt = Instant.now(),
                            ))
                            entriesCreated++
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Failed to fetch rights for space ${space.name}: ${e.message}")
            }
        }

        // 3. Ensure all xWiki groups appear in the matrix (even without explicit rights)
        // This makes them visible in the Groups tab so admins can assign permissions
        val allGroups = xwikiClient.getGroups()
        val existingGroupNames = accessEntryRepository.findByApplication("xwiki")
            .filter { it.principalType == PrincipalType.GROUP }
            .map { it.principalName }
            .toSet()

        allGroups.forEach { group ->
            if (group.name !in existingGroupNames) {
                accessEntryRepository.save(AccessEntry(
                    principalType = PrincipalType.GROUP,
                    principalName = group.name,
                    application = "xwiki",
                    resourceType = "wiki",
                    resourceName = "(global)",
                    permission = "(none)",
                    allow = false,
                    source = "synced",
                    syncedAt = Instant.now(),
                ))
                entriesCreated++
            }
        }

        logger.info("Access Matrix sync complete: $entriesCreated entries created")

        return ResponseEntity.ok(mapOf(
            "success" to true,
            "entriesCreated" to entriesCreated,
            "globalRights" to globalRights.size,
            "spacesScanned" to relevantSpaces.size,
            "groupsDiscovered" to allGroups.size,
        ))
    }
}
