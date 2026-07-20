package org.opencrowd.core.repository

import org.opencrowd.core.entity.AccessRequest
import org.opencrowd.core.entity.AccessRequestStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AccessRequestRepository : JpaRepository<AccessRequest, UUID> {

    fun findByStatus(status: AccessRequestStatus, pageable: Pageable): Page<AccessRequest>

    fun findByRequestorName(requestorName: String): List<AccessRequest>

    fun findByRequestorUserId(userId: UUID): List<AccessRequest>

    fun countByStatus(status: AccessRequestStatus): Long
}
