package org.opencrowd.core.entity

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import java.util.UUID

/**
 * Extends BaseEntity with audit fields tracking who created/modified the entity.
 */
@MappedSuperclass
abstract class AuditableEntity : BaseEntity() {

    @Column(name = "created_by")
    var createdBy: UUID? = null

    @Column(name = "updated_by")
    var updatedBy: UUID? = null
}
