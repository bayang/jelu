package io.github.bayang.jelu.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI {
        val server = Server()
        server.url = "/"
        return OpenAPI()
            .info(
                Info()
                    .title("Jelu API")
                    .version("v1.0")
                    .license(License().name("MIT").url("https://github.com/bayang/jelu/blob/main/LICENSE"))
            )
            .externalDocs(
                ExternalDocumentation()
                    .description("jelu documentation")
                    .url("https://github.com/bayang/jelu")
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        "basicAuth",
                        SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")
                    )
            )
            .servers(mutableListOf(server))
    }
}
