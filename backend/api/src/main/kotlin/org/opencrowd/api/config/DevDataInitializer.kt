package org.opencrowd.api.config

import org.opencrowd.core.service.TenantProvisioningService
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * Provisions the default 'acme' tenant schema on startup in dev mode.
 * In production, tenant provisioning is done via the admin API.
 */
@Component
@Profile("dev")
class DevDataInitializer(
    private val tenantProvisioningService: TenantProvisioningService
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(DevDataInitializer::class.java)

    override fun run(args: ApplicationArguments?) {
        if (!tenantProvisioningService.tenantExists("acme")) {
            logger.info("Provisioning default dev tenant: acme")
            tenantProvisioningService.provisionTenant("acme")
            logger.info("Dev tenant 'acme' provisioned successfully")
        } else {
            logger.info("Dev tenant 'acme' already exists")
        }
    }
}
