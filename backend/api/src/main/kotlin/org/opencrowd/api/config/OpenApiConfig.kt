package org.opencrowd.api.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI {
        val securitySchemeName = "bearer-jwt"

        return OpenAPI()
            .info(
                Info()
                    .title("OpenCrowd API")
                    .description("Identity & Access Governance for the Open Source World")
                    .version("0.1.0")
                    .contact(
                        Contact()
                            .name("OpenCrowd")
                            .url("https://opencrowd.io")
                    )
                    .license(
                        License()
                            .name("AGPL-3.0")
                            .url("https://www.gnu.org/licenses/agpl-3.0.html")
                    )
            )
            .addSecurityItem(SecurityRequirement().addList(securitySchemeName))
            .components(
                io.swagger.v3.oas.models.Components()
                    .addSecuritySchemes(
                        securitySchemeName,
                        SecurityScheme()
                            .name(securitySchemeName)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("JWT token from Keycloak")
                    )
            )
    }
}
