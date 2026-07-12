package org.opencrowd.api.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/v1")
class HealthController {

    @Value("\${opencrowd.version:0.1.0-SNAPSHOT}")
    private lateinit var version: String

    @GetMapping("/health")
    fun health(): ResponseEntity<HealthResponse> {
        return ResponseEntity.ok(
            HealthResponse(
                status = "UP",
                version = version,
                timestamp = Instant.now()
            )
        )
    }

    @GetMapping("/info")
    fun info(): ResponseEntity<InfoResponse> {
        return ResponseEntity.ok(
            InfoResponse(
                name = "OpenCrowd",
                description = "Identity & Access Governance for the Open Source World",
                version = version,
                timestamp = Instant.now()
            )
        )
    }
}

data class HealthResponse(
    val status: String,
    val version: String,
    val timestamp: Instant
)

data class InfoResponse(
    val name: String,
    val description: String,
    val version: String,
    val timestamp: Instant
)
