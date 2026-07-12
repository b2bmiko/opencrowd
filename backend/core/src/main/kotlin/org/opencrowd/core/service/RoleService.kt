package org.opencrowd.core.service

import org.opencrowd.core.entity.Role
import org.opencrowd.core.entity.RoleScope
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface RoleService {

    fun findById(id: UUID): Role?

    fun findByName(name: String): Role?

    fun findAll(pageable: Pageable): Page<Role>

    fun findByScope(scope: RoleScope, pageable: Pageable): Page<Role>

    fun create(role: Role): Role

    fun update(id: UUID, updater: (Role) -> Role): Role

    fun delete(id: UUID)

    fun assignToUser(roleId: UUID, userId: UUID)

    fun revokeFromUser(roleId: UUID, userId: UUID)
}
