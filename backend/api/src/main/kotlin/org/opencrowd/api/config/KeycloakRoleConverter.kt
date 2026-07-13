package org.opencrowd.api.config

import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt

/**
 * Extracts roles from Keycloak JWT token and converts them to Spring Security authorities.
 *
 * Keycloak stores roles in:
 * - realm_access.roles: ["platform_admin", "tenant_admin", "user"]
 * - resource_access.opencrowd-backend.roles: ["manage_users", "manage_groups", ...]
 *
 * This converter maps them to:
 * - ROLE_platform_admin
 * - ROLE_tenant_admin
 * - ROLE_user
 * - ROLE_manage_users
 * - ROLE_manage_groups
 * - etc.
 */
class KeycloakRoleConverter : Converter<Jwt, Collection<GrantedAuthority>> {

    override fun convert(jwt: Jwt): Collection<GrantedAuthority> {
        val authorities = mutableListOf<GrantedAuthority>()

        // Extract realm roles
        val realmAccess = jwt.getClaimAsMap("realm_access")
        if (realmAccess != null) {
            @Suppress("UNCHECKED_CAST")
            val roles = realmAccess["roles"] as? List<String> ?: emptyList()
            roles.forEach { role ->
                authorities.add(SimpleGrantedAuthority("ROLE_$role"))
            }
        }

        // Extract client roles (opencrowd-backend)
        val resourceAccess = jwt.getClaimAsMap("resource_access")
        if (resourceAccess != null) {
            @Suppress("UNCHECKED_CAST")
            val clientAccess = resourceAccess["opencrowd-backend"] as? Map<String, Any>
            if (clientAccess != null) {
                @Suppress("UNCHECKED_CAST")
                val clientRoles = clientAccess["roles"] as? List<String> ?: emptyList()
                clientRoles.forEach { role ->
                    authorities.add(SimpleGrantedAuthority("ROLE_$role"))
                }
            }
        }

        return authorities
    }
}
