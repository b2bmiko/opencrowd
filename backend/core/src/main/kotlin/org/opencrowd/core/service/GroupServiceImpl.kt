package org.opencrowd.core.service

import org.opencrowd.core.entity.Group
import org.opencrowd.core.entity.GroupType
import org.opencrowd.core.event.DomainEventPublisher
import org.opencrowd.core.event.GroupCreated
import org.opencrowd.core.event.GroupMemberAdded
import org.opencrowd.core.event.GroupMemberRemoved
import org.opencrowd.core.multitenancy.TenantContext
import org.opencrowd.core.repository.GroupRepository
import org.opencrowd.core.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class GroupServiceImpl(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val eventPublisher: DomainEventPublisher,
) : GroupService {

    private val logger = LoggerFactory.getLogger(GroupServiceImpl::class.java)

    @Transactional(readOnly = true)
    override fun findById(id: UUID): Group? = groupRepository.findById(id).orElse(null)

    @Transactional(readOnly = true)
    override fun findByName(name: String): Group? = groupRepository.findByName(name)

    @Transactional(readOnly = true)
    override fun findAll(pageable: Pageable): Page<Group> = groupRepository.findAll(pageable)

    @Transactional(readOnly = true)
    override fun findByType(type: GroupType, pageable: Pageable): Page<Group> =
        groupRepository.findByType(type, pageable)

    @Transactional(readOnly = true)
    override fun findByParentId(parentId: UUID, pageable: Pageable): Page<Group> =
        groupRepository.findByParentId(parentId, pageable)

    override fun create(group: Group): Group {
        groupRepository.findByName(group.name)?.let {
            throw IllegalArgumentException("Group with name '${group.name}' already exists")
        }

        val saved = groupRepository.save(group)
        logger.info("Group created: id=${saved.id}, name=${saved.name}")

        eventPublisher.publish(
            GroupCreated(
                tenantId = TenantContext.getTenantId() ?: "unknown",
                actorId = null,
                correlationId = UUID.randomUUID().toString(),
                groupId = saved.id!!,
                name = saved.name,
            )
        )

        return saved
    }

    override fun update(id: UUID, updater: (Group) -> Group): Group {
        val existing = groupRepository.findById(id)
            .orElseThrow { NoSuchElementException("Group not found: $id") }

        val updated = updater(existing)
        val saved = groupRepository.save(updated)
        logger.info("Group updated: id=${saved.id}, name=${saved.name}")
        return saved
    }

    override fun delete(id: UUID) {
        val group = groupRepository.findById(id)
            .orElseThrow { NoSuchElementException("Group not found: $id") }
        groupRepository.delete(group)
        logger.info("Group deleted: id=$id, name=${group.name}")
    }

    override fun addMember(groupId: UUID, userId: UUID) {
        val group = groupRepository.findById(groupId)
            .orElseThrow { NoSuchElementException("Group not found: $groupId") }
        val user = userRepository.findById(userId)
            .orElseThrow { NoSuchElementException("User not found: $userId") }

        group.members.add(user)
        groupRepository.save(group)
        logger.info("Member added: group=${group.name}, user=${user.username}")

        eventPublisher.publish(
            GroupMemberAdded(
                tenantId = TenantContext.getTenantId() ?: "unknown",
                actorId = null,
                correlationId = UUID.randomUUID().toString(),
                groupId = groupId,
                userId = userId,
            )
        )
    }

    override fun removeMember(groupId: UUID, userId: UUID) {
        val group = groupRepository.findById(groupId)
            .orElseThrow { NoSuchElementException("Group not found: $groupId") }
        val user = userRepository.findById(userId)
            .orElseThrow { NoSuchElementException("User not found: $userId") }

        group.members.remove(user)
        groupRepository.save(group)
        logger.info("Member removed: group=${group.name}, user=${user.username}")

        eventPublisher.publish(
            GroupMemberRemoved(
                tenantId = TenantContext.getTenantId() ?: "unknown",
                actorId = null,
                correlationId = UUID.randomUUID().toString(),
                groupId = groupId,
                userId = userId,
            )
        )
    }

    @Transactional(readOnly = true)
    override fun getMembers(groupId: UUID): Set<UUID> {
        val group = groupRepository.findById(groupId)
            .orElseThrow { NoSuchElementException("Group not found: $groupId") }
        return group.members.mapNotNull { it.id }.toSet()
    }
}
