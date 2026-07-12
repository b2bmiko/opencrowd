package org.opencrowd.core.multitenancy

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class TenantContextTest {

    @AfterEach
    fun cleanup() {
        TenantContext.clear()
    }

    @Test
    fun `should return null when no tenant is set`() {
        assertNull(TenantContext.getTenantId())
    }

    @Test
    fun `should store and retrieve tenant id`() {
        TenantContext.setTenantId("acme")
        assertEquals("acme", TenantContext.getTenantId())
    }

    @Test
    fun `should clear tenant id`() {
        TenantContext.setTenantId("acme")
        TenantContext.clear()
        assertNull(TenantContext.getTenantId())
    }

    @Test
    fun `should throw when requireTenantId is called without tenant set`() {
        assertThrows(IllegalStateException::class.java) {
            TenantContext.requireTenantId()
        }
    }

    @Test
    fun `should return tenant id when requireTenantId is called with tenant set`() {
        TenantContext.setTenantId("globex")
        assertEquals("globex", TenantContext.requireTenantId())
    }

    @Test
    fun `should isolate tenant context between threads`() {
        TenantContext.setTenantId("acme")

        val otherThreadTenant = mutableListOf<String?>()
        val thread = Thread {
            otherThreadTenant.add(TenantContext.getTenantId())
        }
        thread.start()
        thread.join()

        assertEquals("acme", TenantContext.getTenantId())
        assertNull(otherThreadTenant.first())
    }
}
