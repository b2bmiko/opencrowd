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

    private fun parseUserObjects(xml: String): List<XWikiUser> {
        val users = mutableListOf<XWikiUser>()
        try {
            // Parse object summaries to get page references
            val pageRegex = "<headline>(.*?)</headline>".toRegex()
            val idRegex = "<id>(.*?)</id>".toRegex()
            val linkRegex = """<link[^>]*href="([^"]*)"[^>]*rel="http://www\.xwiki\.org/rel/object"[^>]*/>""".toRegex()

            val headlines = pageRegex.findAll(xml).map { it.groupValues[1] }.toList()
            val ids = idRegex.findAll(xml).map { it.groupValues[1] }.toList()

            headlines.forEachIndexed { index, headline ->
                val parts = headline.split(".")
                val username = parts.lastOrNull() ?: headline
                users.add(
                    XWikiUser(
                        username = username,
                        fullName = headline,
                        id = ids.getOrNull(index) ?: "",
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
            val pageRegex = "<headline>(.*?)</headline>".toRegex()
            val headlines = pageRegex.findAll(xml).map { it.groupValues[1] }.toList()

            // Deduplicate by page name (groups appear once per member)
            val uniquePages = headlines.map { it.substringBeforeLast(".") }.distinct()
            uniquePages.forEach { pageName ->
                val name = pageName.substringAfterLast(".")
                groups.add(XWikiGroup(name = name, fullName = pageName))
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
