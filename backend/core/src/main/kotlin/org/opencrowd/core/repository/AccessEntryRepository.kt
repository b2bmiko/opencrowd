package org.opencrowd.core.repository

import org.opencrowd.core.entity.AccessEntry
import org.opencrowd.core.entity.PrincipalType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AccessEntryRepository : JpaRepository<AccessEntry, UUID> {

    fun findByApplication(application: String): List<AccessEntry>

    fun findByPrincipalTypeAndPrincipalName(principalType: PrincipalType, principalName: String): List<AccessEntry>

    fun findByPrincipalName(principalName: String): List<AccessEntry>

    fun findByResourceTypeAndResourceName(resourceType: String, resourceName: String): List<AccessEntry>

    fun findByApplicationAndResourceName(application: String, resourceName: String): List<AccessEntry>

    fun deleteByApplicationAndSource(application: String, source: String)

    fun deleteByConnectorId(connectorId: UUID)
}
