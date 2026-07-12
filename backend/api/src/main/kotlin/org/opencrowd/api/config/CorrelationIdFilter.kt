package org.opencrowd.api.config

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Generates or propagates a correlation ID for every request.
 * The ID is placed in the MDC for structured logging and returned
 * as a response header.
 */
@Component
@Order(0)
class CorrelationIdFilter : Filter {

    companion object {
        const val HEADER_NAME = "X-Correlation-ID"
        const val MDC_KEY = "correlationId"
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        val correlationId = httpRequest.getHeader(HEADER_NAME)
            ?: UUID.randomUUID().toString()

        MDC.put(MDC_KEY, correlationId)
        httpResponse.setHeader(HEADER_NAME, correlationId)

        try {
            chain.doFilter(request, response)
        } finally {
            MDC.remove(MDC_KEY)
        }
    }
}
