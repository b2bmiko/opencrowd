package org.opencrowd.core.service

import org.opencrowd.core.entity.User
import org.opencrowd.core.entity.UserStatus
import org.opencrowd.core.event.DomainEventPublisher
import org.opencrowd.core.event.UserCreated
import org.opencrowd.core.event.UserStatusChanged
import org.opencrowd.core.event.UserUpdated
import org.opencrowd.core.multitenancy.TenantContext
import org.opencrowd.core.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val eventPublisher: DomainEventPublisher,
) : UserService {

    private val logger = LoggerFactory.getLogger(UserServiceImpl::class.java)

    @Transactional(readOnly = true)
    override fun findById(id: UUID): User? = userRepository.findById(id).orElse(null)

    @Transactional(readOnly = true)
    override fun findByEmail(email: String): User? = userRepository.findByEmail(email)

    @Transactional(readOnly = true)
    override fun findByUsername(username: String): User? = userRepository.findByUsername(username)

    @Transactional(readOnly = true)
    override fun findAll(pageable: Pageable): Page<User> = userRepository.findAll(pageable)

    @Transactional(readOnly = true)
    override fun findByStatus(status: UserStatus, pageable: Pageable): Page<User> =
        userRepository.findByStatus(status, pageable)

    @Transactional(readOnly = true)
    override fun findByDepartment(department: String, pageable: Pageable): Page<User> =
        userRepository.findByDepartment(department, pageable)

    override fun create(user: User): User {
        // Validate uniqueness
        userRepository.findByEmail(user.email)?.let {
            throw IllegalArgumentException("User with email '${user.email}' already exists")
        }
        userRepository.findByUsername(user.username)?.let {
            throw IllegalArgumentException("User with username '${user.username}' already exists")
        }

        val saved = userRepository.save(user)
        logger.info("User created: id=${saved.id}, username=${saved.username}, email=${saved.email}")

        eventPublisher.publish(
            UserCreated(
                tenantId = TenantContext.getTenantId() ?: "unknown",
                actorId = null,
                correlationId = UUID.randomUUID().toString(),
                userId = saved.id!!,
                email = saved.email,
                username = saved.username,
            )
        )

        return saved
    }

    override fun update(id: UUID, updater: (User) -> User): User {
        val existing = userRepository.findById(id)
            .orElseThrow { NoSuchElementException("User not found: $id") }

        val updated = updater(existing)
        val saved = userRepository.save(updated)

        logger.info("User updated: id=${saved.id}, username=${saved.username}")

        eventPublisher.publish(
            UserUpdated(
                tenantId = TenantContext.getTenantId() ?: "unknown",
                actorId = null,
                correlationId = UUID.randomUUID().toString(),
                userId = saved.id!!,
                changes = emptyMap(), // Could diff fields here
            )
        )

        return saved
    }

    override fun changeStatus(id: UUID, newStatus: UserStatus): User {
        val user = userRepository.findById(id)
            .orElseThrow { NoSuchElementException("User not found: $id") }

        val previousStatus = user.status

        // Validate state transitions
        validateStatusTransition(previousStatus, newStatus)

        user.status = newStatus
        val saved = userRepository.save(user)

        logger.info("User status changed: id=${saved.id}, ${previousStatus} → ${newStatus}")

        eventPublisher.publish(
            UserStatusChanged(
                tenantId = TenantContext.getTenantId() ?: "unknown",
                actorId = null,
                correlationId = UUID.randomUUID().toString(),
                userId = saved.id!!,
                previousStatus = previousStatus.name,
                newStatus = newStatus.name,
            )
        )

        return saved
    }

    override fun delete(id: UUID) {
        val user = userRepository.findById(id)
            .orElseThrow { NoSuchElementException("User not found: $id") }
        userRepository.delete(user)
        logger.info("User deleted: id=$id, username=${user.username}")
    }

    @Transactional(readOnly = true)
    override fun countByStatus(status: UserStatus): Long = userRepository.countByStatus(status)

    /**
     * Validates user lifecycle state transitions.
     * 
     * Valid transitions:
     * PENDING → ACTIVE
     * ACTIVE → DISABLED, LOCKED, OFFBOARDED
     * DISABLED → ACTIVE, OFFBOARDED
     * LOCKED → ACTIVE, DISABLED
     * OFFBOARDED → (none, terminal state)
     */
    private fun validateStatusTransition(from: UserStatus, to: UserStatus) {
        val validTransitions: Map<UserStatus, Set<UserStatus>> = mapOf(
            UserStatus.PENDING to setOf(UserStatus.ACTIVE),
            UserStatus.ACTIVE to setOf(UserStatus.DISABLED, UserStatus.LOCKED, UserStatus.OFFBOARDED),
            UserStatus.DISABLED to setOf(UserStatus.ACTIVE, UserStatus.OFFBOARDED),
            UserStatus.LOCKED to setOf(UserStatus.ACTIVE, UserStatus.DISABLED),
            UserStatus.OFFBOARDED to emptySet(),
        )

        val allowed = validTransitions[from] ?: emptySet()
        if (to !in allowed) {
            throw IllegalArgumentException(
                "Invalid status transition: $from → $to. Allowed transitions from $from: $allowed"
            )
        }
    }
}
