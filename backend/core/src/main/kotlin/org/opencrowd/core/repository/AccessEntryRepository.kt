package org.opencrowd.core.repository

import org.opencrowd.core.entity.AccessEntry
import org.opencrowd.core.entity.PrincipalType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
interface AccessEntryRepository : JpaRepository<AccessEntry, UUID> {

    fun findByApplication(application: String): List<AccessEntry>

    fun findByPrincipalTypeAndPrincipalName(principalType: PrincipalType, principalName: String): List<AccessEntry>

    fun findByPrincipalName(principalName: String): List<AccessEntry>

    fun findByResourceTypeAndResourceName(resourceType: String, resourceName: String): List<AccessEntry>

    fun findByApplicationAndResourceName(application: String, resourceName: String): List<AccessEntry>

    @Modifying
    @Transactional
    @Query("DELETE FROM AccessEntry a WHERE a.application = :application AND a.source = :source")
    fun deleteByApplicationAndSource(application: String, source: String)

    @Modifying
    @Transactional
    @Query("DELETE FROM AccessEntry a WHERE a.connectorId = :connectorId")
    fun deleteByConnectorId(connectorId: UUID)
}
