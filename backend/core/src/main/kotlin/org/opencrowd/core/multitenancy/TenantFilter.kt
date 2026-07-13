package org.opencrowd.core.multitenancy

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.annotation.Order
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

/**
 * Servlet filter that extracts the tenant_id from:
 * 1. The JWT token's tenant_id claim (primary)
 * 2. X-Tenant-ID request header (fallback for dev/testing)
 *
 * Order is set high (100) to ensure it runs AFTER Spring Security
 * has populated the SecurityContextHolder with the JWT.
 */
@Component
@Order(100)
class TenantFilter : Filter {

    companion object {
        const val TENANT_CLAIM = "tenant_id"
        const val TENANT_HEADER = "X-Tenant-ID"
        const val DEFAULT_TENANT = "acme"
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        try {
            val httpRequest = request as HttpServletRequest
            val tenantId = extractTenantFromJwt() 
                ?: httpRequest.getHeader(TENANT_HEADER) 
                ?: DEFAULT_TENANT
            TenantContext.setTenantId(tenantId)
            chain.doFilter(request, response)
        } finally {
            TenantContext.clear()
        }
    }

    private fun extractTenantFromJwt(): String? {
        val authentication = SecurityContextHolder.getContext().authentication ?: return null
        val principal = authentication.principal
        if (principal is Jwt) {
            return principal.getClaimAsString(TENANT_CLAIM)
        }
        return null
    }
}
