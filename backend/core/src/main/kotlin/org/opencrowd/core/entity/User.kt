package org.opencrowd.core.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "users")
class User(

    @Column(name = "external_id", unique = true)
    var externalId: String? = null,

    @Column(name = "username", unique = true, nullable = false)
    var username: String,

    @Column(name = "email", unique = true, nullable = false)
    var email: String,

    @Column(name = "first_name")
    var firstName: String? = null,

    @Column(name = "last_name")
    var lastName: String? = null,

    @Column(name = "display_name")
    var displayName: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: UserStatus = UserStatus.ACTIVE,

    @Column(name = "department")
    var department: String? = null,

    @Column(name = "title")
    var title: String? = null,

    @Column(name = "phone")
    var phone: String? = null,

    @Column(name = "avatar_url")
    var avatarUrl: String? = null,

    @Column(name = "last_login_at")
    var lastLoginAt: Instant? = null,

) : AuditableEntity()

enum class UserStatus {
    ACTIVE,
    DISABLED,
    LOCKED,
    PENDING,
    OFFBOARDED
}
