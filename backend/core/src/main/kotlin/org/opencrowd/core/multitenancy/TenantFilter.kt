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
 * Servlet filter that extracts the tenant_id claim from the JWT token
 * and sets it in TenantContext for the duration of the request.
 */
@Component
@Order(1)
class TenantFilter : Filter {

    companion object {
        const val TENANT_CLAIM = "tenant_id"
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        try {
            val tenantId = extractTenantId(request as HttpServletRequest)
            if (tenantId != null) {
                TenantContext.setTenantId(tenantId)
            }
            chain.doFilter(request, response)
        } finally {
            TenantContext.clear()
        }
    }

    private fun extractTenantId(request: HttpServletRequest): String? {
        val authentication = SecurityContextHolder.getContext().authentication ?: return null
        val principal = authentication.principal
        if (principal is Jwt) {
            return principal.getClaimAsString(TENANT_CLAIM)
        }
        return null
    }
}
