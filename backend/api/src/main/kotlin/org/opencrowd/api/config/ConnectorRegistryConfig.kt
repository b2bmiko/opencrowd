package org.opencrowd.api.config

import org.opencrowd.connectors.sdk.Connector
import org.opencrowd.connectors.sdk.ConnectorRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ConnectorRegistryConfig {

    @Bean
    fun connectorRegistry(connectors: List<Connector>): ConnectorRegistry =
        ConnectorRegistry(connectors)
}
