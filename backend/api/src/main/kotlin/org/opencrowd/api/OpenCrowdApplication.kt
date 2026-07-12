package org.opencrowd.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["org.opencrowd"])
class OpenCrowdApplication

fun main(args: Array<String>) {
    runApplication<OpenCrowdApplication>(*args)
}
