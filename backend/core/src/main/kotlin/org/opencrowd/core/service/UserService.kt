package org.opencrowd.core.service

import org.opencrowd.core.entity.User
import org.opencrowd.core.entity.UserStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface UserService {

    fun findById(id: UUID): User?

    fun findByEmail(email: String): User?

    fun findByUsername(username: String): User?

    fun findAll(pageable: Pageable): Page<User>

    fun findByStatus(status: UserStatus, pageable: Pageable): Page<User>

    fun findByDepartment(department: String, pageable: Pageable): Page<User>

    fun create(user: User): User

    fun update(id: UUID, updater: (User) -> User): User

    fun changeStatus(id: UUID, newStatus: UserStatus): User

    fun delete(id: UUID)

    fun countByStatus(status: UserStatus): Long
}
