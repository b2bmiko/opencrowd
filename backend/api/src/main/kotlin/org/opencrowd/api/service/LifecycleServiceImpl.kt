package org.opencrowd.api.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.opencrowd.connectors.sdk.ConnectorRegistry
import org.opencrowd.connectors.xwiki.XWikiClient
import org.opencrowd.core.entity.ConnectorStatus
import org.opencrowd.core.entity.User
import org.opencrowd.core.entity.UserStatus
import org.opencrowd.core.event.DomainEventPublisher
import org.opencrowd.core.event.UserCreated
import org.opencrowd.core.event.UserStatusChanged
import org.opencrowd.core.multitenancy.TenantContext
import org.opencrowd.core.service.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class LifecycleServiceImpl(
    private val userService: UserService,
    private val groupService: GroupService,
    private val connectorService: ConnectorService,
    private val connectorRegistry: ConnectorRegistry,
    private val eventPublisher: DomainEventPublisher,
) : LifecycleService {

    private val logger = LoggerFactory.getLogger(LifecycleServiceImpl::class.java)
    private val objectMapper = jacksonObjectMapper()

    override fun executeJoiner(user: User, groupIds: List<UUID>, connectorIds: List<UUID>?): JoinerResult {
        val correlationId = UUID.randomUUID().toString()
        val errors = mutableListOf<String>()

        val createdUser = userService.create(user)
        val userId = createdUser.id!!

        logger.info("[JML:Joiner] User created: ${createdUser.username} (${createdUser.id})")

        val assignedGroups = mutableListOf<String>()
        groupIds.forEach { groupId ->
            try {
                groupService.addMember(groupId, userId)
                val group = groupService.findById(groupId)
                assignedGroups.add(group?.name ?: groupId.toString())
            } catch (e: Exception) {
                errors.add("Failed to assign group $groupId: ${e.message}")
                logger.warn("[JML:Joiner] Group assignment failed: $groupId - ${e.message}")
            }
        }

        val provisioningResults = provisionUser(createdUser, connectorIds)
        provisioningResults.filter { it.action == "failed" }.forEach {
            errors.add("${it.connectorName}: ${it.message}")
        }

        eventPublisher.publish(
            UserCreated(
                tenantId = TenantContext.getTenantId() ?: "default",
                actorId = null,
                correlationId = correlationId,
                userId = userId,
                email = createdUser.email,
                username = createdUser.username,
            )
        )

        val success = errors.isEmpty()
        logger.info("[JML:Joiner] Complete: ${createdUser.username}, groups=${assignedGroups.size}, connectors=${provisioningResults.size}, success=$success")

        return JoinerResult(
            userId = userId,
            username = createdUser.username,
            groupsAssigned = assignedGroups,
            provisioningResults = provisioningResults,
            success = success,
            errors = errors,
        )
    }

    override fun executeLeaver(userId: UUID, connectorIds: List<UUID>?): LeaverResult {
        val correlationId = UUID.randomUUID().toString()
        val errors = mutableListOf<String>()

        val user = userService.findById(userId)
            ?: throw NoSuchElementException("User not found: $userId")

        if (user.status == UserStatus.OFFBOARDED) {
            throw IllegalStateException("User ${user.username} is already offboarded")
        }

        val previousStatus = user.status.name
        logger.info("[JML:Leaver] Starting offboarding for: ${user.username} (status: $previousStatus)")

        val removedGroups = mutableListOf<String>()
        val allGroups = groupService.findAll(org.springframework.data.domain.PageRequest.of(0, 500))
        allGroups.content.forEach { group ->
            try {
                val members = groupService.getMembers(group.id!!)
                if (userId in members) {
                    groupService.removeMember(group.id!!, userId)
                    removedGroups.add(group.name)
                }
            } catch (e: Exception) {
                errors.add("Failed to remove from group ${group.name}: ${e.message}")
            }
        }

        val deprovisioningResults = deprovisionUser(user, connectorIds)
        deprovisioningResults.filter { it.action == "failed" }.forEach {
            errors.add("${it.connectorName}: ${it.message}")
        }

        userService.changeStatus(userId, UserStatus.OFFBOARDED)

        eventPublisher.publish(
            UserStatusChanged(
                tenantId = TenantContext.getTenantId() ?: "default",
                actorId = null,
                correlationId = correlationId,
                userId = userId,
                previousStatus = previousStatus,
                newStatus = UserStatus.OFFBOARDED.name,
            )
        )

        val success = errors.isEmpty()
        logger.info("[JML:Leaver] Complete: ${user.username}, groupsRemoved=${removedGroups.size}, connectors=${deprovisioningResults.size}, success=$success")

        return LeaverResult(
            userId = userId,
            username = user.username,
            groupsRemoved = removedGroups,
            deprovisioningResults = deprovisioningResults,
            success = success,
            errors = errors,
        )
    }

    override fun previewJoiner(user: User, groupIds: List<UUID>, connectorIds: List<UUID>?): JoinerPreview {
        val groupNames = groupIds.mapNotNull { groupService.findById(it)?.name }
        val connectors = getTargetConnectors(connectorIds)
        return JoinerPreview(
            user = user,
            groupsToAssign = groupNames,
            connectorsToProvision = connectors.map { "${it.name} (${it.connectorType})" },
        )
    }

    override fun previewLeaver(userId: UUID, connectorIds: List<UUID>?): LeaverPreview {
        val user = userService.findById(userId)
            ?: throw NoSuchElementException("User not found: $userId")

        val groupNames = mutableListOf<String>()
        val allGroups = groupService.findAll(org.springframework.data.domain.PageRequest.of(0, 500))
        allGroups.content.forEach { group ->
            val members = groupService.getMembers(group.id!!)
            if (userId in members) {
                groupNames.add(group.name)
            }
        }

        val connectors = getTargetConnectors(connectorIds)

        return LeaverPreview(
            userId = userId,
            username = user.username,
            groupsToRemove = groupNames,
            connectorsToDeprovision = connectors.map { "${it.name} (${it.connectorType})" },
            currentStatus = user.status,
        )
    }

    override fun reactivateUser(userId: UUID, groupIds: List<UUID>): JoinerResult {
        val user = userService.findById(userId)
            ?: throw NoSuchElementException("User not found: $userId")

        if (user.status != UserStatus.OFFBOARDED && user.status != UserStatus.DISABLED) {
            throw IllegalStateException("User ${user.username} is not offboarded/disabled (status: ${user.status})")
        }

        userService.changeStatus(userId, UserStatus.ACTIVE)

        val assignedGroups = mutableListOf<String>()
        val errors = mutableListOf<String>()
        groupIds.forEach { groupId ->
            try {
                groupService.addMember(groupId, userId)
                val group = groupService.findById(groupId)
                assignedGroups.add(group?.name ?: groupId.toString())
            } catch (e: Exception) {
                errors.add("Failed to assign group $groupId: ${e.message}")
            }
        }

        val provisioningResults = provisionUser(user, null)
        provisioningResults.filter { it.action == "failed" }.forEach {
            errors.add("${it.connectorName}: ${it.message}")
        }

        return JoinerResult(
            userId = userId,
            username = user.username,
            groupsAssigned = assignedGroups,
            provisioningResults = provisioningResults,
            success = errors.isEmpty(),
            errors = errors,
        )
    }

    // --- Private helpers ---

    private fun provisionUser(user: User, connectorIds: List<UUID>?): List<ProvisioningResult> {
        val connectors = getTargetConnectors(connectorIds)
        return connectors.map { dbConnector ->
            try {
                when (dbConnector.connectorType) {
                    "xwiki" -> provisionToXWiki(user, dbConnector)
                    else -> ProvisioningResult(dbConnector.id!!, dbConnector.name, dbConnector.connectorType, "skipped", "Not implemented for ${dbConnector.connectorType}")
                }
            } catch (e: Exception) {
                ProvisioningResult(dbConnector.id!!, dbConnector.name, dbConnector.connectorType, "failed", e.message)
            }
        }
    }

    private fun deprovisionUser(user: User, connectorIds: List<UUID>?): List<ProvisioningResult> {
        val connectors = getTargetConnectors(connectorIds)
        return connectors.map { dbConnector ->
            try {
                when (dbConnector.connectorType) {
                    "xwiki" -> deprovisionFromXWiki(user, dbConnector)
                    else -> ProvisioningResult(dbConnector.id!!, dbConnector.name, dbConnector.connectorType, "skipped", "Not implemented for ${dbConnector.connectorType}")
                }
            } catch (e: Exception) {
                ProvisioningResult(dbConnector.id!!, dbConnector.name, dbConnector.connectorType, "failed", e.message)
            }
        }
    }

    private fun provisionToXWiki(user: User, dbConnector: org.opencrowd.core.entity.Connector): ProvisioningResult {
        val client = getXWikiClient(dbConnector)
            ?: return ProvisioningResult(dbConnector.id!!, dbConnector.name, "xwiki", "failed", "No credentials configured")

        val success = client.createUser(user.username, user.email, user.firstName, user.lastName, null)
        return ProvisioningResult(dbConnector.id!!, dbConnector.name, "xwiki",
            if (success) "provisioned" else "failed",
            if (success) "User created in xWiki" else "Failed to create user in xWiki")
    }

    private fun deprovisionFromXWiki(user: User, dbConnector: org.opencrowd.core.entity.Connector): ProvisioningResult {
        val client = getXWikiClient(dbConnector)
            ?: return ProvisioningResult(dbConnector.id!!, dbConnector.name, "xwiki", "failed", "No credentials configured")

        val success = client.disableUser(user.username)
        return ProvisioningResult(dbConnector.id!!, dbConnector.name, "xwiki",
            if (success) "deprovisioned" else "failed",
            if (success) "User disabled in xWiki" else "Failed to disable user in xWiki")
    }

    private fun getXWikiClient(dbConnector: org.opencrowd.core.entity.Connector): XWikiClient? {
        if (dbConnector.config == "{}") return null
        return try {
            val configMap = objectMapper.readValue<Map<String, String>>(dbConnector.config)
            val baseUrl = configMap["baseUrl"] ?: return null
            val username = configMap["username"] ?: return null
            val password = configMap["password"] ?: return null
            XWikiClient(baseUrl.trimEnd('/'), username, password)
        } catch (e: Exception) {
            logger.error("Failed to create xWiki client for connector ${dbConnector.name}: ${e.message}")
            null
        }
    }

    private fun getTargetConnectors(connectorIds: List<UUID>?): List<org.opencrowd.core.entity.Connector> {
        val allConnectors = connectorService.findAll()
        val connected = allConnectors.filter { it.status == ConnectorStatus.CONNECTED }
        return if (connectorIds != null) connected.filter { it.id in connectorIds } else connected
    }
}
