package org.opencrowd.core.repository

import org.opencrowd.core.entity.User
import org.opencrowd.core.entity.UserStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserRepository : JpaRepository<User, UUID> {

    fun findByEmail(email: String): User?

    fun findByUsername(username: String): User?

    fun findByExternalId(externalId: String): User?

    fun findByStatus(status: UserStatus, pageable: Pageable): Page<User>

    fun findByDepartment(department: String, pageable: Pageable): Page<User>

    fun countByStatus(status: UserStatus): Long
}
