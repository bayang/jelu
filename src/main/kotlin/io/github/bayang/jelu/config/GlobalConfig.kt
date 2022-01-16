package io.github.bayang.jelu.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

const val sessionHeaderName: String = "X-Auth-Token"

@Configuration
class GlobalConfig {

    @Bean("passwordEncoder")
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun corsConfigurationSource(jeluProperties: JeluProperties): UrlBasedCorsConfigurationSource =
        UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration(
                "/**",
                CorsConfiguration().applyPermitDefaultValues().apply {
                    allowedOrigins = if (jeluProperties.cors.allowedOrigins.isNullOrEmpty()) null else jeluProperties.cors.allowedOrigins
                    allowedMethods = HttpMethod.values().map { it.name }
                    allowCredentials = true
                    addExposedHeader(HttpHeaders.CONTENT_DISPOSITION)
                    addExposedHeader(sessionHeaderName)
                },
            )
        }
}
