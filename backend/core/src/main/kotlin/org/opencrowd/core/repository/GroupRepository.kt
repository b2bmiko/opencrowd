package org.opencrowd.core.repository

import org.opencrowd.core.entity.Group
import org.opencrowd.core.entity.GroupType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface GroupRepository : JpaRepository<Group, UUID> {

    fun findByName(name: String): Group?

    fun findByType(type: GroupType, pageable: Pageable): Page<Group>

    fun findByParentId(parentId: UUID, pageable: Pageable): Page<Group>

    fun findByOwnerId(ownerId: UUID): List<Group>

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(gm) FROM Group g JOIN g.members gm WHERE g.id = :groupId")
    fun countMembers(groupId: UUID): Long
}
