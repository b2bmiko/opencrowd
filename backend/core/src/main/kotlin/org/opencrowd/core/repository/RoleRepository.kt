package org.opencrowd.core.repository

import org.opencrowd.core.entity.Role
import org.opencrowd.core.entity.RoleScope
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface RoleRepository : JpaRepository<Role, UUID> {

    fun findByName(name: String): Role?

    fun findByScope(scope: RoleScope, pageable: Pageable): Page<Role>

    fun findByParentId(parentId: UUID): List<Role>
}
