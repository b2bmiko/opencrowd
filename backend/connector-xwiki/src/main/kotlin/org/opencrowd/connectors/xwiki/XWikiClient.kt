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
     * Fetch all users from xWiki.
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

        logger.info("Fetched ${users.size} users from xWiki")
        return users
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
