package org.opencrowd.core.service

import org.opencrowd.core.entity.Group
import org.opencrowd.core.entity.GroupType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface GroupService {

    fun findById(id: UUID): Group?

    fun findByName(name: String): Group?

    fun findAll(pageable: Pageable): Page<Group>

    fun findByType(type: GroupType, pageable: Pageable): Page<Group>

    fun findByParentId(parentId: UUID, pageable: Pageable): Page<Group>

    fun create(group: Group): Group

    fun update(id: UUID, updater: (Group) -> Group): Group

    fun delete(id: UUID)

    fun addMember(groupId: UUID, userId: UUID)

    fun removeMember(groupId: UUID, userId: UUID)

    fun getMembers(groupId: UUID): Set<UUID>
}
