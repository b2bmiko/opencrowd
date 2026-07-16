package org.opencrowd.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
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

    @PostMapping("/sync-rights")
    @Operation(summary = "Sync rights from xWiki", description = "Fetches permissions from xWiki and stores them in the Access Matrix")
    @PreAuthorize("hasRole('manage_connectors')")
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
            // Skip system/internal spaces
            !space.name.startsWith("Help") &&
            !space.name.startsWith("Menu") &&
            !space.name.startsWith("Sandbox") &&
            !space.name.startsWith("XWikiCrowd") &&
            space.name != "XWiki" &&
            space.name != "Main" &&
            !space.name.contains(".")
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

        logger.info("Access Matrix sync complete: $entriesCreated entries created")

        return ResponseEntity.ok(mapOf(
            "success" to true,
            "entriesCreated" to entriesCreated,
            "globalRights" to globalRights.size,
            "spacesScanned" to relevantSpaces.size,
        ))
    }
}
