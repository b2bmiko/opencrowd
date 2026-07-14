package org.opencrowd.core.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

@Entity
@Table(name = "roles")
class Role(

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false)
    var scope: RoleScope = RoleScope.GLOBAL,

    @Column(name = "parent_id")
    var parentId: UUID? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "permissions", columnDefinition = "jsonb", nullable = false)
    var permissions: String = "[]",

) : AuditableEntity()

enum class RoleScope {
    GLOBAL,
    APPLICATION,
    RESOURCE
}
