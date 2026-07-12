package org.opencrowd.core.event

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * Wraps Spring's ApplicationEventPublisher to provide a typed interface
 * for publishing domain events. In Phase 2, this can be extended to also
 * publish to a message broker (RabbitMQ/Kafka).
 */
@Component
class DomainEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    fun publish(event: DomainEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}
