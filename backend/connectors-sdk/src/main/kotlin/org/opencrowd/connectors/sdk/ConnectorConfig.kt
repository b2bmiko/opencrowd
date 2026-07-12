package org.opencrowd.connectors.sdk

/**
 * Configuration for connecting to an external application.
 */
data class ConnectorConfig(
    val baseUrl: String,
    val authentication: AuthConfig,
    val syncSchedule: String? = null,
    val options: Map<String, String> = emptyMap()
)

/**
 * Authentication configuration for connectors.
 */
sealed class AuthConfig {
    data class BasicAuth(val username: String, val password: String) : AuthConfig()
    data class BearerToken(val token: String) : AuthConfig()
    data class OAuth2(
        val clientId: String,
        val clientSecret: String,
        val tokenUrl: String,
        val scopes: List<String> = emptyList()
    ) : AuthConfig()
    data class ApiKey(val header: String, val key: String) : AuthConfig()
}
