package org.opencrowd.core.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "access_requests")
class AccessRequest(

    @Column(name = "requestor_name", nullable = false)
    var requestorName: String,

    @Column(name = "requestor_email")
    var requestorEmail: String? = null,

    @Column(name = "requestor_user_id")
    var requestorUserId: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false)
    var requestType: AccessRequestType = AccessRequestType.ACCESS,

    @Column(name = "application", nullable = false)
    var application: String = "xwiki",

    @Column(name = "resource_name", nullable = false)
    var resourceName: String = "(global)",

    @Column(name = "permission", nullable = false)
    var permission: String,

    @Column(name = "justification", columnDefinition = "TEXT")
    var justification: String? = null,

    @Column(name = "custom_fields", columnDefinition = "TEXT")
    var customFields: String? = null, // JSON string for custom form fields

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: AccessRequestStatus = AccessRequestStatus.PENDING,

    @Column(name = "reviewer_name")
    var reviewerName: String? = null,

    @Column(name = "reviewer_id")
    var reviewerId: UUID? = null,

    @Column(name = "reviewed_at")
    var reviewedAt: Instant? = null,

    @Column(name = "review_comment")
    var reviewComment: String? = null,

    @Column(name = "expires_at")
    var expiresAt: Instant? = null,

) : AuditableEntity()

enum class AccessRequestType {
    ACCESS,
    REMOVAL,
    ELEVATION,
    TEMPORARY
}

enum class AccessRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    EXPIRED,
    CANCELLED
}
