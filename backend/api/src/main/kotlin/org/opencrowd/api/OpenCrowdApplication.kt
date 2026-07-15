package org.opencrowd.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication(scanBasePackages = ["org.opencrowd"])
@EntityScan(basePackages = ["org.opencrowd.core.entity"])
@EnableJpaRepositories(basePackages = ["org.opencrowd.core.repository"])
@EnableAsync
class OpenCrowdApplication

fun main(args: Array<String>) {
    runApplication<OpenCrowdApplication>(*args)
}
