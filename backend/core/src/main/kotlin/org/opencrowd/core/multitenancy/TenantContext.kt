package org.opencrowd.core.multitenancy

/**
 * Holds the current tenant identifier for the executing thread.
 * Set by TenantFilter from the JWT token's tenant_id claim.
 */
object TenantContext {

    private val currentTenant = ThreadLocal<String?>()

    fun setTenantId(tenantId: String?) {
        currentTenant.set(tenantId)
    }

    fun getTenantId(): String? = currentTenant.get()

    fun requireTenantId(): String =
        currentTenant.get() ?: throw IllegalStateException("No tenant context set for current thread")

    fun clear() {
        currentTenant.remove()
    }
}
