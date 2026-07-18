package org.opencrowd.connectors.xwiki

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Base64

/**
 * HTTP client for xWiki REST API.
 * Handles authentication and XML response parsing.
 */
class XWikiClient(
    private val baseUrl: String,
    private val username: String,
    private val password: String,
) {

    private val logger = LoggerFactory.getLogger(XWikiClient::class.java)
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
    private val xmlMapper = XmlMapper().registerKotlinModule()

    private val authHeader: String = "Basic " + Base64.getEncoder()
        .encodeToString("$username:$password".toByteArray())

    /**
     * Test the connection to xWiki.
     * @return true if the API is reachable and credentials are valid
     */
    fun testConnection(): Boolean {
        return try {
            val response = get("/rest")
            response.statusCode() == 200
        } catch (e: Exception) {
            logger.error("xWiki connection test failed: ${e.message}")
            false
        }
    }

    /**
     * Get xWiki version info.
     */
    fun getVersion(): String? {
        return try {
            val response = get("/rest")
            if (response.statusCode() == 200) {
                val body = response.body()
                // Extract version from XML response
                val versionRegex = "<version>(.*?)</version>".toRegex()
                versionRegex.find(body)?.groupValues?.get(1)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Create a user in xWiki.
     * Creates the user page and sets the XWikiUsers object with properties.
     */
    fun createUser(username: String, email: String, firstName: String?, lastName: String?, password: String?): Boolean {
        return try {
            // Step 1: Create user page content
            val content = """
                {{include reference="XWiki.XWikiUserSheet"/}}
            """.trimIndent()

            val pageXml = """
                <page xmlns="http://www.xwiki.org">
                    <title>$username</title>
                    <content>$content</content>
                </page>
            """.trimIndent()

            val pageResponse = put("/rest/wikis/xwiki/spaces/XWiki/pages/$username", pageXml)
            if (pageResponse.statusCode() !in 200..299) {
                logger.error("Failed to create xWiki user page: HTTP ${pageResponse.statusCode()}")
                return false
            }

            // Step 2: Create XWikiUsers object with properties
            val propsXml = buildString {
                append("""<object xmlns="http://www.xwiki.org">""")
                append("<className>XWiki.XWikiUsers</className>")
                append("<property><email>$email</email></property>")
                if (firstName != null) append("<property><first_name>$firstName</first_name></property>")
                if (lastName != null) append("<property><last_name>$lastName</last_name></property>")
                if (password != null) append("<property><password>$password</password></property>")
                append("<property><active>1</active></property>")
                append("</object>")
            }

            val objResponse = post("/rest/wikis/xwiki/spaces/XWiki/pages/$username/objects", propsXml)
            if (objResponse.statusCode() !in 200..299) {
                logger.error("Failed to create xWiki user object: HTTP ${objResponse.statusCode()}")
                return false
            }

            logger.info("Created user in xWiki: $username")
            true
        } catch (e: Exception) {
            logger.error("Failed to create user in xWiki: ${e.message}")
            false
        }
    }

    /**
     * Fetch all users from xWiki with full profile details.
     */
    fun getUsers(wiki: String = "xwiki"): List<XWikiUser> {
        val users = mutableListOf<XWikiUser>()
        var offset = 0
        val limit = 100

        while (true) {
            val response = get("/rest/wikis/$wiki/classes/XWiki.XWikiUsers/objects?start=$offset&number=$limit")
            if (response.statusCode() != 200) {
                logger.error("Failed to fetch users: HTTP ${response.statusCode()}")
                break
            }

            val body = response.body()
            val pageUsers = parseUserObjects(body)
            if (pageUsers.isEmpty()) break

            users.addAll(pageUsers)
            offset += limit

            if (pageUsers.size < limit) break
        }

        // Fetch full details (email, firstName, lastName) for each user
        val detailedUsers = users.map { user ->
            try {
                getUserDetails(user.username, wiki) ?: user
            } catch (e: Exception) {
                logger.warn("Failed to fetch details for user ${user.username}: ${e.message}")
                user
            }
        }

        logger.info("Fetched ${detailedUsers.size} users from xWiki (with details)")
        return detailedUsers
    }

    /**
     * Fetch full profile details for a single user from their XWikiUsers object.
     * Tries object index 0 first, falls back to listing objects to find the right one.
     */
    fun getUserDetails(username: String, wiki: String = "xwiki"): XWikiUser? {
        // Try the common case: object at index 0
        val response = get("/rest/wikis/$wiki/spaces/XWiki/pages/$username/objects/XWiki.XWikiUsers/0")
        if (response.statusCode() == 200) {
            return parseUserDetailResponse(response.body(), username)
        }

        // Fallback: list all XWikiUsers objects on this page and use the first one
        val listResponse = get("/rest/wikis/$wiki/spaces/XWiki/pages/$username/objects/XWiki.XWikiUsers")
        if (listResponse.statusCode() == 200) {
            val numberRegex = "<number>(\\d+)</number>".toRegex()
            val numbers = numberRegex.findAll(listResponse.body()).map { it.groupValues[1].toInt() }.toList()
            if (numbers.isNotEmpty()) {
                val detailResponse = get("/rest/wikis/$wiki/spaces/XWiki/pages/$username/objects/XWiki.XWikiUsers/${numbers.first()}")
                if (detailResponse.statusCode() == 200) {
                    return parseUserDetailResponse(detailResponse.body(), username)
                }
            }
        }

        return null
    }

    private fun parseUserDetailResponse(body: String, username: String): XWikiUser {
        val email = extractPropertyValue(body, "email")
        val firstName = extractPropertyValue(body, "first_name")
        val lastName = extractPropertyValue(body, "last_name")

        return XWikiUser(
            username = username,
            fullName = "XWiki.$username",
            id = username,
            email = email?.takeIf { it.isNotBlank() },
            firstName = firstName?.takeIf { it.isNotBlank() },
            lastName = lastName?.takeIf { it.isNotBlank() },
        )
    }

    /**
     * Fetch all groups from xWiki.
     */
    fun getGroups(wiki: String = "xwiki"): List<XWikiGroup> {
        val response = get("/rest/wikis/$wiki/classes/XWiki.XWikiGroups/objects")
        if (response.statusCode() != 200) {
            logger.error("Failed to fetch groups: HTTP ${response.statusCode()}")
            return emptyList()
        }

        val body = response.body()
        val groups = parseGroupObjects(body)
        logger.info("Fetched ${groups.size} groups from xWiki")
        return groups
    }

    /**
     * Add a permission right in xWiki for a user or group on a space.
     * Creates or updates an XWikiRights object on the space's WebPreferences page.
     */
    fun addRight(spaceName: String, principalName: String, isGroup: Boolean, levels: List<String>, wiki: String = "xwiki"): Boolean {
        return try {
            val principal = "XWiki.$principalName"
            val levelsStr = levels.joinToString(",")

            val xmlBody = buildString {
                append("""<object xmlns="http://www.xwiki.org">""")
                append("<className>XWiki.XWikiRights</className>")
                append("""<property name="levels"><value>$levelsStr</value></property>""")
                if (isGroup) {
                    append("""<property name="groups"><value>$principal</value></property>""")
                    append("""<property name="users"><value></value></property>""")
                } else {
                    append("""<property name="users"><value>$principal</value></property>""")
                    append("""<property name="groups"><value></value></property>""")
                }
                append("""<property name="allow"><value>1</value></property>""")
                append("</object>")
            }

            val path = if (spaceName == "(global)") {
                "/rest/wikis/$wiki/spaces/XWiki/pages/XWikiPreferences/objects"
            } else {
                "/rest/wikis/$wiki/spaces/$spaceName/pages/WebPreferences/objects"
            }

            val response = post(path, xmlBody)
            if (response.statusCode() in 200..299) {
                logger.info("Added right: $principalName → $levelsStr on $spaceName")
                true
            } else {
                logger.error("Failed to add right: HTTP ${response.statusCode()} - ${response.body().take(200)}")
                false
            }
        } catch (e: Exception) {
            logger.error("Failed to add right: ${e.message}")
            false
        }
    }

    /**
     * Remove a permission right in xWiki for a user or group on a space.
     * Finds the matching XWikiRights object and deletes it.
     */
    fun removeRight(spaceName: String, principalName: String, isGroup: Boolean, levels: List<String>, wiki: String = "xwiki"): Boolean {
        return try {
            val principal = "XWiki.$principalName"

            // Determine the correct path based on global vs space-level
            val basePath = if (spaceName == "(global)") {
                "/rest/wikis/$wiki/spaces/XWiki/pages/XWikiPreferences/objects/XWiki.XWikiGlobalRights"
            } else {
                "/rest/wikis/$wiki/spaces/$spaceName/pages/WebPreferences/objects/XWiki.XWikiRights"
            }

            // List all rights objects to find the matching one
            val listResponse = get(basePath)
            if (listResponse.statusCode() != 200) {
                logger.error("Failed to list rights for removal: HTTP ${listResponse.statusCode()}")
                return false
            }

            val body = listResponse.body()
            val numberRegex = "<number>(\\d+)</number>".toRegex()
            val numbers = numberRegex.findAll(body).map { it.groupValues[1].toInt() }.toList()

            // Check each rights object to find the one matching our criteria
            for (num in numbers) {
                val detailResponse = get("$basePath/$num")
                if (detailResponse.statusCode() != 200) continue

                val detailBody = detailResponse.body()
                val usersValue = extractPropertyValue(detailBody, "users") ?: ""
                val groupsValue = extractPropertyValue(detailBody, "groups") ?: ""
                val levelsValue = extractPropertyValue(detailBody, "levels") ?: ""

                val currentLevels = levelsValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val matchesPrincipal = if (isGroup) {
                    groupsValue.contains(principalName) || groupsValue.contains(principal)
                } else {
                    usersValue.contains(principalName) || usersValue.contains(principal)
                }
                val matchesLevel = levels.any { it in currentLevels }

                if (matchesPrincipal && matchesLevel) {
                    val deleteResponse = delete("$basePath/$num")
                    if (deleteResponse.statusCode() in 200..299) {
                        logger.info("Removed right #$num: $principalName → ${levels.joinToString(",")} on $spaceName")
                        return true
                    } else {
                        logger.error("Failed to delete right #$num: HTTP ${deleteResponse.statusCode()}")
                        return false
                    }
                }
            }

            logger.warn("No matching right found to remove: $principalName → ${levels.joinToString(",")} on $spaceName")
            false
        } catch (e: Exception) {
            logger.error("Failed to remove right: ${e.message}")
            false
        }
    }

    /**
     * Disable a user in xWiki by setting their active property to 0.
     */
    fun disableUser(username: String, wiki: String = "xwiki"): Boolean {
        return try {
            // Update the active property on the XWikiUsers object
            val xmlBody = """
                <property xmlns="http://www.xwiki.org">
                    <value>0</value>
                </property>
            """.trimIndent()

            val response = put(
                "/rest/wikis/$wiki/spaces/XWiki/pages/$username/objects/XWiki.XWikiUsers/0/properties/active",
                xmlBody
            )

            if (response.statusCode() in 200..299) {
                logger.info("Disabled user in xWiki: $username")
                true
            } else {
                logger.error("Failed to disable xWiki user $username: HTTP ${response.statusCode()}")
                false
            }
        } catch (e: Exception) {
            logger.error("Failed to disable user in xWiki: ${e.message}")
            false
        }
    }

    /**
     * Fetch members of a specific group.
     */
    fun getGroupMembers(groupName: String, wiki: String = "xwiki"): List<String> {
        val response = get("/rest/wikis/$wiki/spaces/XWiki/pages/$groupName/objects/XWiki.XWikiGroups")
        if (response.statusCode() != 200) {
            logger.error("Failed to fetch group members for $groupName: HTTP ${response.statusCode()}")
            return emptyList()
        }

        val body = response.body()
        val members = mutableListOf<String>()

        // Each objectSummary with a non-empty <headline> is a member reference (XWiki.username)
        val headlineRegex = "<headline>(.*?)</headline>".toRegex()
        headlineRegex.findAll(body).forEach { match ->
            val value = match.groupValues[1].trim()
            if (value.isNotEmpty() && value.startsWith("XWiki.")) {
                val username = value.removePrefix("XWiki.")
                members.add(username)
            }
        }

        logger.debug("Group '$groupName' has ${members.size} members: $members")
        return members
    }

    /**
     * Fetch rights/permissions for a specific space.
     * Returns both XWikiRights (space-level) entries.
     */
    fun getSpaceRights(spaceName: String, wiki: String = "xwiki"): List<XWikiRight> {
        val rights = mutableListOf<XWikiRight>()

        // Fetch space-level rights
        val response = get("/rest/wikis/$wiki/spaces/$spaceName/pages/WebPreferences/objects/XWiki.XWikiRights")
        if (response.statusCode() == 200 && !response.body().contains("<objects xmlns=\"http://www.xwiki.org\"/>")) {
            // Get the count of rights objects
            val numberRegex = "<number>(\\d+)</number>".toRegex()
            val numbers = numberRegex.findAll(response.body()).map { it.groupValues[1].toInt() }.toList()

            numbers.forEach { num ->
                val detailResponse = get("/rest/wikis/$wiki/spaces/$spaceName/pages/WebPreferences/objects/XWiki.XWikiRights/$num")
                if (detailResponse.statusCode() == 200) {
                    val right = parseRightObject(detailResponse.body(), spaceName)
                    if (right != null) rights.add(right)
                }
            }
        }

        logger.info("Found ${rights.size} rights entries for space '$spaceName'")
        return rights
    }

    /**
     * Fetch global wiki-level rights.
     * xWiki stores wiki-level rights in XWiki.XWikiPreferences page.
     */
    fun getGlobalRights(wiki: String = "xwiki"): List<XWikiRight> {
        val rights = mutableListOf<XWikiRight>()

        // Wiki-level rights are in XWikiPreferences (this is where the Rights admin page stores them)
        val response = get("/rest/wikis/$wiki/spaces/XWiki/pages/XWikiPreferences/objects/XWiki.XWikiGlobalRights")
        if (response.statusCode() == 200 && !response.body().contains("<objects xmlns=\"http://www.xwiki.org\"/>")) {
            val numberRegex = "<number>(\\d+)</number>".toRegex()
            val numbers = numberRegex.findAll(response.body()).map { it.groupValues[1].toInt() }.toList()

            numbers.forEach { num ->
                val detailResponse = get("/rest/wikis/$wiki/spaces/XWiki/pages/XWikiPreferences/objects/XWiki.XWikiGlobalRights/$num")
                if (detailResponse.statusCode() == 200) {
                    val right = parseRightObject(detailResponse.body(), "(global)")
                    if (right != null) rights.add(right)
                }
            }
        }

        // Also check WebPreferences (older location)
        val response2 = get("/rest/wikis/$wiki/spaces/XWiki/pages/WebPreferences/objects/XWiki.XWikiGlobalRights")
        if (response2.statusCode() == 200 && !response2.body().contains("<objects xmlns=\"http://www.xwiki.org\"/>")) {
            val numberRegex = "<number>(\\d+)</number>".toRegex()
            val numbers = numberRegex.findAll(response2.body()).map { it.groupValues[1].toInt() }.toList()

            numbers.forEach { num ->
                val detailResponse = get("/rest/wikis/$wiki/spaces/XWiki/pages/WebPreferences/objects/XWiki.XWikiGlobalRights/$num")
                if (detailResponse.statusCode() == 200) {
                    val right = parseRightObject(detailResponse.body(), "(global)")
                    if (right != null) rights.add(right)
                }
            }
        }

        logger.info("Found ${rights.size} global rights entries")
        return rights
    }

    private fun parseRightObject(xml: String, spaceName: String): XWikiRight? {
        return try {
            val usersValue = extractPropertyValue(xml, "users")
            val groupsValue = extractPropertyValue(xml, "groups")
            val levelsValue = extractPropertyValue(xml, "levels")
            val allowValue = extractPropertyValue(xml, "allow")

            if (levelsValue.isNullOrEmpty()) return null

            val levels = levelsValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val users = if (!usersValue.isNullOrEmpty()) usersValue.split(",").map { it.trim().removePrefix("XWiki.") }.filter { it.isNotEmpty() } else emptyList()
            val groups = if (!groupsValue.isNullOrEmpty()) groupsValue.split(",").map { it.trim().removePrefix("XWiki.") }.filter { it.isNotEmpty() } else emptyList()
            val allow = allowValue == "1"

            XWikiRight(
                spaceName = spaceName,
                users = users,
                groups = groups,
                levels = levels,
                allow = allow,
            )
        } catch (e: Exception) {
            logger.error("Failed to parse right object: ${e.message}")
            null
        }
    }

    private fun extractPropertyValue(xml: String, propertyName: String): String? {
        // Find the property block and extract its <value> element
        val propertyRegex = """<property name="$propertyName"[^>]*>.*?<value>(.*?)</value>.*?</property>""".toRegex(RegexOption.DOT_MATCHES_ALL)
        return propertyRegex.find(xml)?.groupValues?.get(1)?.trim()
    }

    /**
     * Fetch spaces (wikis/pages structure).
     */
    fun getSpaces(wiki: String = "xwiki"): List<XWikiSpace> {
        val response = get("/rest/wikis/$wiki/spaces")
        if (response.statusCode() != 200) {
            logger.error("Failed to fetch spaces: HTTP ${response.statusCode()}")
            return emptyList()
        }

        val body = response.body()
        val spaces = parseSpaces(body)
        logger.info("Fetched ${spaces.size} spaces from xWiki")
        return spaces
    }

    private fun get(path: String): HttpResponse<String> {
        val url = if (path.startsWith("http")) path else "$baseUrl$path"
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", authHeader)
            .header("Accept", "application/xml")
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build()

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun put(path: String, xmlBody: String): HttpResponse<String> {
        val url = if (path.startsWith("http")) path else "$baseUrl$path"
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", authHeader)
            .header("Content-Type", "application/xml")
            .header("Accept", "application/xml")
            .timeout(Duration.ofSeconds(30))
            .PUT(HttpRequest.BodyPublishers.ofString(xmlBody))
            .build()

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun post(path: String, xmlBody: String): HttpResponse<String> {
        val url = if (path.startsWith("http")) path else "$baseUrl$path"
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", authHeader)
            .header("Content-Type", "application/xml")
            .header("Accept", "application/xml")
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(xmlBody))
            .build()

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun delete(path: String): HttpResponse<String> {
        val url = if (path.startsWith("http")) path else "$baseUrl$path"
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", authHeader)
            .header("Accept", "application/xml")
            .timeout(Duration.ofSeconds(30))
            .DELETE()
            .build()

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun parseUserObjects(xml: String): List<XWikiUser> {
        val users = mutableListOf<XWikiUser>()
        try {
            // Parse objectSummary elements - extract pageName as username
            val pageNameRegex = "<pageName>(.*?)</pageName>".toRegex()
            val idRegex = "<id>(.*?)</id>".toRegex()
            val guiIdRegex = "<guid>(.*?)</guid>".toRegex()

            val pageNames = pageNameRegex.findAll(xml).map { it.groupValues[1] }.toList()
            val guids = guiIdRegex.findAll(xml).map { it.groupValues[1] }.toList()

            pageNames.forEachIndexed { index, pageName ->
                users.add(
                    XWikiUser(
                        username = pageName,
                        fullName = "XWiki.$pageName",
                        id = guids.getOrNull(index) ?: "",
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to parse user objects: ${e.message}")
        }
        return users
    }

    private fun parseGroupObjects(xml: String): List<XWikiGroup> {
        val groups = mutableListOf<XWikiGroup>()
        try {
            val pageNameRegex = "<pageName>(.*?)</pageName>".toRegex()
            val pageNames = pageNameRegex.findAll(xml).map { it.groupValues[1] }.toList()

            // Deduplicate (groups appear once per member in xWiki)
            val uniqueNames = pageNames.distinct()
            uniqueNames.forEach { name ->
                groups.add(XWikiGroup(name = name, fullName = "XWiki.$name"))
            }
        } catch (e: Exception) {
            logger.error("Failed to parse group objects: ${e.message}")
        }
        return groups
    }

    private fun parseSpaces(xml: String): List<XWikiSpace> {
        val spaces = mutableListOf<XWikiSpace>()
        try {
            val nameRegex = "<name>(.*?)</name>".toRegex()
            val names = nameRegex.findAll(xml).map { it.groupValues[1] }.toList()
            names.forEach { name ->
                spaces.add(XWikiSpace(name = name))
            }
        } catch (e: Exception) {
            logger.error("Failed to parse spaces: ${e.message}")
        }
        return spaces
    }
}

// Data models for xWiki responses
data class XWikiUser(
    val username: String,
    val fullName: String,
    val id: String = "",
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
)

data class XWikiGroup(
    val name: String,
    val fullName: String,
    val members: List<String> = emptyList(),
)

data class XWikiSpace(
    val name: String,
    val url: String? = null,
)

data class XWikiRight(
    val spaceName: String,
    val users: List<String>,
    val groups: List<String>,
    val levels: List<String>,
    val allow: Boolean,
)
