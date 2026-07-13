package org.opencrowd.api.config

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Helper to extract user information from the current security context.
 * Use this in services/controllers to get the authenticated user's details.
 */
@Component
class SecurityContextHelper {

    fun getCurrentJwt(): Jwt? {
        val authentication = SecurityContextHolder.getContext().authentication ?: return null
        val principal = authentication.principal
        return if (principal is Jwt) principal else null
    }

    fun getCurrentUserId(): UUID? {
        val jwt = getCurrentJwt() ?: return null
        return try {
            UUID.fromString(jwt.subject)
        } catch (_: Exception) {
            null
        }
    }

    fun getCurrentUserEmail(): String? {
        return getCurrentJwt()?.getClaimAsString("email")
    }

    fun getCurrentUsername(): String? {
        return getCurrentJwt()?.getClaimAsString("preferred_username")
    }

    fun getCurrentTenantId(): String? {
        return getCurrentJwt()?.getClaimAsString("tenant_id")
    }

    fun hasRole(role: String): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication ?: return false
        return authentication.authorities.any { it.authority == "ROLE_$role" }
    }

    fun isPlatformAdmin(): Boolean = hasRole("platform_admin")

    fun isTenantAdmin(): Boolean = hasRole("tenant_admin")
}
