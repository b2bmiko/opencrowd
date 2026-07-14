package org.opencrowd.core.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

@Entity
@Table(name = "groups")
class Group(

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    var type: GroupType = GroupType.STATIC,

    @Column(name = "parent_id")
    var parentId: UUID? = null,

    @Column(name = "owner_id")
    var ownerId: UUID? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dynamic_filter", columnDefinition = "jsonb")
    var dynamicFilter: String? = null,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "group_members",
        joinColumns = [JoinColumn(name = "group_id")],
        inverseJoinColumns = [JoinColumn(name = "user_id")]
    )
    var members: MutableSet<User> = mutableSetOf(),

) : AuditableEntity()

enum class GroupType {
    STATIC,
    DYNAMIC
}
