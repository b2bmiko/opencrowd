package org.opencrowd.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.opencrowd.core.entity.AccessRequestStatus
import org.opencrowd.core.multitenancy.TenantContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Kai — AI Governance Assistant.
 * Uses Mistral AI (EU-compliant) when API key is configured,
 * falls back to rule-based responses otherwise.
 * Can query OpenCrowd's database for real-time answers about users, groups, permissions.
 */
@RestController
@RequestMapping("/api/v1/assistant")
@Tag(name = "Kai Assistant", description = "AI-powered governance assistant")
class AssistantController(
    @Value("\${opencrowd.ai.mistral-api-key:}") private val mistralApiKey: String,
    private val userService: org.opencrowd.core.service.UserService,
    private val groupService: org.opencrowd.core.service.GroupService,
    private val connectorService: org.opencrowd.core.service.ConnectorService,
    private val accessEntryRepository: org.opencrowd.core.repository.AccessEntryRepository,
    private val accessRequestRepository: org.opencrowd.core.repository.AccessRequestRepository,
) {

    private val logger = LoggerFactory.getLogger(AssistantController::class.java)
    private val httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()

    companion object {
        private const val MISTRAL_URL = "https://api.mistral.ai/v1/chat/completions"
        private const val MODEL = "mistral-small-latest"

        private val SYSTEM_PROMPT = """
You are Kai, the AI assistant for OpenCrowd — an open-source Identity & Access Governance platform.

Your role:
- Help users understand how to request access, join groups, and navigate OpenCrowd
- Explain permissions (view, comment, edit, delete, admin, script, register, programming)
- Guide admins on onboarding/offboarding, syncing with xWiki, managing connectors
- Be concise, friendly, and practical

OpenCrowd features you know about:
- Dashboard: overview with stats
- Applications: connect external apps (xWiki, OpenProject, Nextcloud) via REST API
- Identity: manage users, onboard (Joiner flow), offboard (Leaver flow)
- Groups: organize users, sync memberships from connected apps
- Access Matrix: unified permission grid across all apps, grant/revoke with one click
- Access Profiles: predefined templates (permissions + groups) for quick role assignment
- Requests: users submit access requests, admins approve/reject
- Audit: full history of all changes
- Reports: governance score, compliance checklist
- Settings: sync interval, defaults, auto-provisioning
- Auto-sync: runs every 30 minutes to keep everything in sync

Key workflows:
- New joiner: Identity → Onboard User → pick Access Profile → user created + provisioned
- Access request: /request page (public) → admin reviews → approve grants permission
- Offboarding: Identity → user → Offboard → removes from groups + disables in apps
- Sync: Applications → Sync button → imports users/groups/memberships from xWiki

Keep responses short (2-4 paragraphs max). Use bullet points for lists. Be helpful.
        """.trimIndent()
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat with Kai", description = "Send a message to the AI governance assistant")
    fun chat(@RequestBody body: ChatRequest): ResponseEntity<Map<String, String>> {
        ensureTenantContext()
        val userMessage = body.message

        // First: try to answer from data (deterministic, fast, free)
        val dataAnswer = queryData(userMessage)
        if (dataAnswer != null) {
            return ResponseEntity.ok(mapOf("reply" to dataAnswer))
        }

        // Second: try Mistral AI if API key is configured
        if (mistralApiKey.isNotBlank()) {
            try {
                val reply = callMistral(userMessage, body.history ?: emptyList())
                return ResponseEntity.ok(mapOf("reply" to reply))
            } catch (e: Exception) {
                logger.warn("[Kai] Mistral call failed, using fallback: ${e.message}")
            }
        }

        // Fallback: rule-based response
        val reply = getLocalResponse(userMessage)
        return ResponseEntity.ok(mapOf("reply" to reply))
    }

    /**
     * Queries OpenCrowd's database to answer factual questions.
     * Returns null if the question doesn't match a data query pattern.
     */
    private fun queryData(input: String): String? {
        val q = input.lowercase()

        // "permissions of X" / "what can X do" / "what rights does X have"
        val permissionPatterns = listOf(
            """(?:permissions?|rights?|access)\s+(?:of|for)\s+(\w+)""".toRegex(),
            """what\s+(?:can|does|permissions?|rights?)\s+(\w+)\s+(?:do|have|get)""".toRegex(),
            """(?:show|list|get)\s+(?:permissions?|rights?|access)\s+(?:of|for)\s+(\w+)""".toRegex(),
        )
        for (pattern in permissionPatterns) {
            val match = pattern.find(q)
            if (match != null) {
                val name = match.groupValues[1]
                return lookupPermissions(name)
            }
        }

        // "groups of X" / "what groups is X in"
        val groupPatterns = listOf(
            """(?:groups?)\s+(?:of|for)\s+(\w+)""".toRegex(),
            """what\s+groups?\s+(?:is|does)\s+(\w+)""".toRegex(),
            """(\w+)\s+(?:groups?|member)""".toRegex(),
        )
        for (pattern in groupPatterns) {
            val match = pattern.find(q)
            if (match != null) {
                val name = match.groupValues[1]
                if (name !in listOf("the", "all", "my", "a", "in", "is", "what", "which", "how", "many")) {
                    return lookupGroups(name)
                }
            }
        }

        // "who has admin" / "who has X permission"
        if (q.contains("who has") || q.contains("who have")) {
            val permMatch = """who\s+(?:has|have)\s+(\w+)""".toRegex().find(q)
            if (permMatch != null) {
                val perm = permMatch.groupValues[1]
                return lookupWhoHasPermission(perm)
            }
        }

        // "how many users" / "how many groups"
        if (q.contains("how many users") || q.contains("total users") || q.contains("user count")) {
            return getStats()
        }
        if (q.contains("how many groups") || q.contains("total groups") || q.contains("group count")) {
            val groups = groupService.findAll(PageRequest.of(0, 1))
            return "There are **${groups.totalElements}** groups in OpenCrowd."
        }

        // "list all users" / "show users"
        if ((q.contains("list") || q.contains("show") || q.contains("all")) && q.contains("users")) {
            return listUsers()
        }

        // "list all groups" / "show groups"
        if ((q.contains("list") || q.contains("show") || q.contains("all")) && q.contains("groups")) {
            return listGroups()
        }

        // "pending requests"
        if (q.contains("pending") && q.contains("request")) {
            val count = accessRequestRepository.countByStatus(AccessRequestStatus.PENDING)
            return if (count > 0L) "There are **$count** pending access requests waiting for review." else "No pending requests — all caught up!"
        }

        // "status" / "stats" / "overview"
        if (q.contains("status") || q.contains("stats") || q.contains("overview") || q.contains("summary")) {
            return getStats()
        }

        return null
    }

    private fun lookupPermissions(name: String): String {
        val entries = accessEntryRepository.findByPrincipalName(name)
            .filter { it.permission != "(none)" && it.allow }

        if (entries.isEmpty()) {
            // Try case-insensitive search
            val allEntries = accessEntryRepository.findAll()
            val matched = allEntries.filter { it.principalName.equals(name, ignoreCase = true) && it.permission != "(none)" && it.allow }
            if (matched.isEmpty()) {
                return "I couldn't find any permissions for \"$name\". Check the exact username in the Identity page."
            }
            val principalName = matched.first().principalName
            val byResource = matched.groupBy { it.resourceName }
            return formatPermissions(principalName, byResource)
        }

        val byResource = entries.groupBy { it.resourceName }
        return formatPermissions(name, byResource)
    }

    private fun formatPermissions(name: String, byResource: Map<String, List<org.opencrowd.core.entity.AccessEntry>>): String {
        val sb = StringBuilder("**Permissions for $name:**\n\n")
        byResource.forEach { (resource, perms) ->
            sb.append("📁 **$resource**: ${perms.map { it.permission }.joinToString(", ")}\n")
        }
        sb.append("\nTotal: ${byResource.values.sumOf { it.size }} permission(s) across ${byResource.size} resource(s).")
        return sb.toString()
    }

    private fun lookupGroups(name: String): String {
        // Find the user
        val user = userService.findByUsername(name)
            ?: userService.findAll(PageRequest.of(0, 200)).content.find { it.username.equals(name, ignoreCase = true) || it.displayName?.contains(name, ignoreCase = true) == true }

        if (user == null) {
            return "I couldn't find a user matching \"$name\". Check the Identity page for the exact username."
        }

        val allGroups = groupService.findAll(PageRequest.of(0, 200))
        val memberOf = mutableListOf<String>()
        allGroups.content.forEach { group ->
            val members = groupService.getMembers(group.id!!)
            if (user.id!! in members) {
                memberOf.add(group.name)
            }
        }

        return if (memberOf.isNotEmpty()) {
            "**${user.displayName ?: user.username}** is a member of ${memberOf.size} group(s):\n\n${memberOf.joinToString("\n") { "• $it" }}"
        } else {
            "**${user.displayName ?: user.username}** is not a member of any groups."
        }
    }

    private fun lookupWhoHasPermission(permission: String): String {
        val entries = accessEntryRepository.findAll()
            .filter { it.permission.equals(permission, ignoreCase = true) && it.allow && it.permission != "(none)" }

        if (entries.isEmpty()) {
            return "No users or groups currently have the \"$permission\" permission."
        }

        val users = entries.filter { it.principalType == org.opencrowd.core.entity.PrincipalType.USER }.map { it.principalName }.distinct()
        val groups = entries.filter { it.principalType == org.opencrowd.core.entity.PrincipalType.GROUP }.map { it.principalName }.distinct()

        val sb = StringBuilder("**Who has \"$permission\" permission:**\n\n")
        if (users.isNotEmpty()) {
            sb.append("👤 Users: ${users.joinToString(", ")}\n")
        }
        if (groups.isNotEmpty()) {
            sb.append("👥 Groups: ${groups.joinToString(", ")}\n")
        }
        sb.append("\nTotal: ${users.size} user(s), ${groups.size} group(s)")
        return sb.toString()
    }

    private fun listUsers(): String {
        val users = userService.findAll(PageRequest.of(0, 50))
        if (users.totalElements == 0L) return "No users in the system yet."

        val sb = StringBuilder("**Users (${users.totalElements} total):**\n\n")
        users.content.forEach { user ->
            val status = if (user.status.name != "ACTIVE") " [${user.status}]" else ""
            sb.append("• **${user.displayName ?: user.username}** (@${user.username})$status\n")
        }
        return sb.toString()
    }

    private fun listGroups(): String {
        val groups = groupService.findAll(PageRequest.of(0, 50))
        if (groups.totalElements == 0L) return "No groups defined yet."

        val sb = StringBuilder("**Groups (${groups.totalElements} total):**\n\n")
        groups.content.forEach { group ->
            sb.append("• **${group.name}**${group.description?.let { " — $it" } ?: ""}\n")
        }
        return sb.toString()
    }

    private fun getStats(): String {
        val usersPage = userService.findAll(PageRequest.of(0, 1))
        val groupsPage = groupService.findAll(PageRequest.of(0, 1))
        val entries = accessEntryRepository.findAll().filter { it.permission != "(none)" }
        val connectors = connectorService.findAll()
        val pendingRequests = accessRequestRepository.countByStatus(AccessRequestStatus.PENDING)

        return """**OpenCrowd Status:**

👤 Users: ${usersPage.totalElements}
👥 Groups: ${groupsPage.totalElements}
🔐 Permission entries: ${entries.size}
🔌 Connected apps: ${connectors.filter { it.status == org.opencrowd.core.entity.ConnectorStatus.CONNECTED }.size}
📋 Pending requests: $pendingRequests"""
    }

    private fun ensureTenantContext() {
        if (TenantContext.getTenantId() == null) {
            TenantContext.setTenantId("acme")
        }
    }

    private fun callMistral(message: String, history: List<ChatMessage>): String {
        val messages = mutableListOf<String>()
        messages.add("""{"role":"system","content":${escapeJson(SYSTEM_PROMPT)}}""")

        // Add conversation history (last 10 messages)
        history.takeLast(10).forEach { msg ->
            messages.add("""{"role":"${msg.role}","content":${escapeJson(msg.content)}}""")
        }
        messages.add("""{"role":"user","content":${escapeJson(message)}}""")

        val requestBody = """{"model":"$MODEL","messages":[${messages.joinToString(",")}],"max_tokens":500,"temperature":0.7}"""

        val request = HttpRequest.newBuilder()
            .uri(URI.create(MISTRAL_URL))
            .header("Authorization", "Bearer $mistralApiKey")
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) {
            throw RuntimeException("Mistral API returned ${response.statusCode()}: ${response.body().take(200)}")
        }

        // Extract content from response JSON
        val body = response.body()
        val contentRegex = """"content"\s*:\s*"((?:[^"\\]|\\.)*)"""".toRegex()
        val match = contentRegex.findAll(body).lastOrNull()
        return match?.groupValues?.get(1)
            ?.replace("\\n", "\n")
            ?.replace("\\\"", "\"")
            ?.replace("\\\\", "\\")
            ?: "I'm having trouble responding right now. Please try again."
    }

    private fun escapeJson(text: String): String {
        return "\"" + text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t") + "\""
    }

    private fun getLocalResponse(input: String): String {
        val q = input.lowercase()

        return when {
            q.contains("request") && (q.contains("access") || q.contains("how")) ->
                "To request access:\n\n1. Go to the public request page at /request\n2. Fill in your name, email, the application, and permission needed\n3. Add a justification\n4. Submit — an admin will review\n\nAdmins can also share the request link directly via email."

            q.contains("group") && (q.contains("what") || q.contains("how") || q.contains("join")) ->
                "Groups organize users by team or role. Each group can have permissions assigned in the Access Matrix.\n\nTo join a group, ask your admin or submit an access request mentioning the group name."

            q.contains("permission") || q.contains("rights") ->
                "Permissions in OpenCrowd:\n\n• **View** — read content\n• **Comment** — add comments\n• **Edit** — modify content\n• **Delete** — remove content\n• **Admin** — full management\n• **Script** — run automations\n\nManaged in the Access Matrix page."

            q.contains("new") && (q.contains("user") || q.contains("employee") || q.contains("joiner")) ->
                "To onboard a new user:\n\n1. Go to Identity → Onboard User\n2. Fill in their details\n3. Select an Access Profile (predefined template)\n4. System creates user + assigns groups + provisions to apps\n\nOr share the /request link for self-service."

            q.contains("offboard") || q.contains("leaver") ->
                "To offboard: Identity → click user → Offboard button. This removes them from all groups and disables their account in connected applications."

            q.contains("sync") || q.contains("xwiki") ->
                "Sync keeps OpenCrowd and xWiki in sync. It runs automatically every 30 minutes, or you can click Sync manually on the Applications page."

            q.contains("profile") || q.contains("template") ->
                "Access Profiles bundle permissions + groups into templates. When onboarding, select a profile and all permissions are granted at once. Manage them on the Access Profiles page."

            q.contains("hello") || q.contains("hi") || q.contains("hey") ->
                "Hey! I'm Kai. I can help you with access requests, permissions, onboarding, groups, and more. What do you need?"

            else ->
                "I can help with:\n\n• Requesting access\n• Understanding permissions\n• Onboarding/offboarding\n• Syncing with apps\n• Access profiles\n\nCould you tell me more about what you need?"
        }
    }
}

data class ChatRequest(
    val message: String,
    val history: List<ChatMessage>? = null,
)

data class ChatMessage(
    val role: String,
    val content: String,
)
