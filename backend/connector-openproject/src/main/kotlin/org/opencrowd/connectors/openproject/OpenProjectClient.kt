package org.opencrowd.connectors.openproject

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Base64

/**
 * HTTP client for OpenProject API v3.
 * Handles authentication (API key via Basic Auth) and JSON response parsing.
 *
 * Auth: Basic Auth with username "apikey" and the API key as password.
 * Base URL: https://<host>/api/v3
 */
class OpenProjectClient(
    private val baseUrl: String,
    private val apiKey: String,
) {

    private val logger = LoggerFactory.getLogger(OpenProjectClient::class.java)
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
    private val mapper = jacksonObjectMapper()

    private val authHeader: String = "Basic " + Base64.getEncoder()
        .encodeToString("apikey:$apiKey".toByteArray())

    /**
     * Test connection to OpenProject.
     */
    fun testConnection(): Boolean {
        return try {
            val response = get("/api/v3")
            response.statusCode() == 200
        } catch (e: Exception) {
            logger.error("OpenProject connection test failed: ${e.message}")
            false
        }
    }

    /**
     * Get OpenProject instance info (version, name).
     */
    fun getInstanceInfo(): Map<String, Any>? {
        return try {
            val response = get("/api/v3")
            if (response.statusCode() == 200) {
                mapper.readValue(response.body())
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Fetch all users from OpenProject.
     */
    fun getUsers(): List<OpenProjectUser> {
        val users = mutableListOf<OpenProjectUser>()
        var offset = 1
        val pageSize = 100

        while (true) {
            val response = get("/api/v3/users?offset=$offset&pageSize=$pageSize")
            if (response.statusCode() != 200) {
                logger.error("Failed to fetch users: HTTP ${response.statusCode()}")
                break
            }

            val body = mapper.readValue<Map<String, Any>>(response.body())
            val embedded = body["_embedded"] as? Map<*, *> ?: break
            val elements = embedded["elements"] as? List<*> ?: break

            if (elements.isEmpty()) break

            elements.forEach { element ->
                val el = element as? Map<*, *> ?: return@forEach
                users.add(OpenProjectUser(
                    id = (el["id"] as? Number)?.toInt() ?: 0,
                    login = el["login"] as? String ?: "",
                    firstName = el["firstName"] as? String,
                    lastName = el["lastName"] as? String,
                    email = el["email"] as? String,
                    status = el["status"] as? String ?: "active",
                    admin = el["admin"] as? Boolean ?: false,
                ))
            }

            if (elements.size < pageSize) break
            offset += pageSize
        }

        logger.info("Fetched ${users.size} users from OpenProject")
        return users
    }

    /**
     * Fetch all groups from OpenProject.
     */
    fun getGroups(): List<OpenProjectGroup> {
        val groups = mutableListOf<OpenProjectGroup>()

        val response = get("/api/v3/groups")
        if (response.statusCode() != 200) {
            logger.error("Failed to fetch groups: HTTP ${response.statusCode()}")
            return emptyList()
        }

        val body = mapper.readValue<Map<String, Any>>(response.body())
        val embedded = body["_embedded"] as? Map<*, *> ?: return emptyList()
        val elements = embedded["elements"] as? List<*> ?: return emptyList()

        elements.forEach { element ->
            val el = element as? Map<*, *> ?: return@forEach
            groups.add(OpenProjectGroup(
                id = (el["id"] as? Number)?.toInt() ?: 0,
                name = el["name"] as? String ?: "",
            ))
        }

        logger.info("Fetched ${groups.size} groups from OpenProject")
        return groups
    }

    /**
     * Fetch all projects from OpenProject.
     */
    fun getProjects(): List<OpenProjectProject> {
        val projects = mutableListOf<OpenProjectProject>()
        var offset = 1
        val pageSize = 100

        while (true) {
            val response = get("/api/v3/projects?offset=$offset&pageSize=$pageSize")
            if (response.statusCode() != 200) {
                logger.error("Failed to fetch projects: HTTP ${response.statusCode()}")
                break
            }

            val body = mapper.readValue<Map<String, Any>>(response.body())
            val embedded = body["_embedded"] as? Map<*, *> ?: break
            val elements = embedded["elements"] as? List<*> ?: break

            if (elements.isEmpty()) break

            elements.forEach { element ->
                val el = element as? Map<*, *> ?: return@forEach
                projects.add(OpenProjectProject(
                    id = (el["id"] as? Number)?.toInt() ?: 0,
                    name = el["name"] as? String ?: "",
                    identifier = el["identifier"] as? String ?: "",
                    description = (el["description"] as? Map<*, *>)?.get("raw") as? String,
                    status = el["status"] as? String,
                    isPublic = el["public"] as? Boolean ?: false,
                ))
            }

            if (elements.size < pageSize) break
            offset += pageSize
        }

        logger.info("Fetched ${projects.size} projects from OpenProject")
        return projects
    }

    /**
     * Fetch all roles from OpenProject.
     */
    fun getRoles(): List<OpenProjectRole> {
        val roles = mutableListOf<OpenProjectRole>()

        val response = get("/api/v3/roles")
        if (response.statusCode() != 200) {
            logger.error("Failed to fetch roles: HTTP ${response.statusCode()}")
            return emptyList()
        }

        val body = mapper.readValue<Map<String, Any>>(response.body())
        val embedded = body["_embedded"] as? Map<*, *> ?: return emptyList()
        val elements = embedded["elements"] as? List<*> ?: return emptyList()

        elements.forEach { element ->
            val el = element as? Map<*, *> ?: return@forEach
            roles.add(OpenProjectRole(
                id = (el["id"] as? Number)?.toInt() ?: 0,
                name = el["name"] as? String ?: "",
            ))
        }

        logger.info("Fetched ${roles.size} roles from OpenProject")
        return roles
    }

    /**
     * Fetch project memberships (who has what role in which project).
     */
    fun getMemberships(projectId: Int? = null): List<OpenProjectMembership> {
        val memberships = mutableListOf<OpenProjectMembership>()
        var offset = 1
        val pageSize = 100
        val filter = if (projectId != null) "&filters=[{\"project\":{\"operator\":\"=\",\"values\":[\"$projectId\"]}}]" else ""

        while (true) {
            val response = get("/api/v3/memberships?offset=$offset&pageSize=$pageSize$filter")
            if (response.statusCode() != 200) {
                logger.error("Failed to fetch memberships: HTTP ${response.statusCode()}")
                break
            }

            val body = mapper.readValue<Map<String, Any>>(response.body())
            val embedded = body["_embedded"] as? Map<*, *> ?: break
            val elements = embedded["elements"] as? List<*> ?: break

            if (elements.isEmpty()) break

            elements.forEach { element ->
                val el = element as? Map<*, *> ?: return@forEach
                val links = el["_links"] as? Map<*, *> ?: return@forEach

                val principalLink = links["principal"] as? Map<*, *>
                val projectLink = links["project"] as? Map<*, *>
                val rolesLinks = (links["roles"] as? List<*>)

                val principalHref = principalLink?.get("href") as? String ?: ""
                val principalTitle = principalLink?.get("title") as? String ?: ""
                val projectHref = projectLink?.get("href") as? String ?: ""
                val projectTitle = projectLink?.get("title") as? String ?: ""

                val roleNames = rolesLinks?.mapNotNull { (it as? Map<*, *>)?.get("title") as? String } ?: emptyList()

                val isGroup = principalHref.contains("/groups/")
                val principalId = principalHref.substringAfterLast("/").toIntOrNull() ?: 0

                memberships.add(OpenProjectMembership(
                    id = (el["id"] as? Number)?.toInt() ?: 0,
                    principalId = principalId,
                    principalName = principalTitle,
                    principalType = if (isGroup) "GROUP" else "USER",
                    projectId = projectHref.substringAfterLast("/").toIntOrNull() ?: 0,
                    projectName = projectTitle,
                    roles = roleNames,
                ))
            }

            if (elements.size < pageSize) break
            offset += pageSize
        }

        logger.info("Fetched ${memberships.size} memberships from OpenProject")
        return memberships
    }

    /**
     * Add a user to a project with a specific role.
     */
    fun addMembership(projectId: Int, userId: Int, roleId: Int): Boolean {
        return try {
            val jsonBody = """
            {
                "_links": {
                    "principal": { "href": "/api/v3/users/$userId" },
                    "project": { "href": "/api/v3/projects/$projectId" },
                    "roles": [{ "href": "/api/v3/roles/$roleId" }]
                }
            }
            """.trimIndent()

            val response = post("/api/v3/memberships", jsonBody)
            if (response.statusCode() in 200..299) {
                logger.info("Added membership: user $userId → project $projectId (role $roleId)")
                true
            } else {
                logger.error("Failed to add membership: HTTP ${response.statusCode()} - ${response.body().take(200)}")
                false
            }
        } catch (e: Exception) {
            logger.error("Failed to add membership: ${e.message}")
            false
        }
    }

    /**
     * Remove a membership by ID.
     */
    fun removeMembership(membershipId: Int): Boolean {
        return try {
            val response = delete("/api/v3/memberships/$membershipId")
            response.statusCode() in 200..299
        } catch (e: Exception) {
            logger.error("Failed to remove membership: ${e.message}")
            false
        }
    }

    /**
     * Get members of a specific group.
     */
    fun getGroupMembers(groupId: Int): List<Int> {
        val response = get("/api/v3/groups/$groupId")
        if (response.statusCode() != 200) return emptyList()

        val body = mapper.readValue<Map<String, Any>>(response.body())
        val embedded = body["_embedded"] as? Map<*, *> ?: return emptyList()
        val members = embedded["members"] as? List<*> ?: return emptyList()

        return members.mapNotNull { member ->
            val m = member as? Map<*, *>
            (m?.get("id") as? Number)?.toInt()
        }
    }

    // --- HTTP methods ---

    private fun get(path: String): HttpResponse<String> {
        val url = if (path.startsWith("http")) path else "$baseUrl$path"
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", authHeader)
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build()

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun post(path: String, jsonBody: String): HttpResponse<String> {
        val url = if (path.startsWith("http")) path else "$baseUrl$path"
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", authHeader)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build()

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun delete(path: String): HttpResponse<String> {
        val url = if (path.startsWith("http")) path else "$baseUrl$path"
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", authHeader)
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(30))
            .DELETE()
            .build()

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }
}

// Data models for OpenProject API responses

data class OpenProjectUser(
    val id: Int,
    val login: String,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val status: String,
    val admin: Boolean = false,
)

data class OpenProjectGroup(
    val id: Int,
    val name: String,
)

data class OpenProjectProject(
    val id: Int,
    val name: String,
    val identifier: String,
    val description: String?,
    val status: String?,
    val isPublic: Boolean,
)

data class OpenProjectRole(
    val id: Int,
    val name: String,
)

data class OpenProjectMembership(
    val id: Int,
    val principalId: Int,
    val principalName: String,
    val principalType: String, // "USER" or "GROUP"
    val projectId: Int,
    val projectName: String,
    val roles: List<String>,
)
