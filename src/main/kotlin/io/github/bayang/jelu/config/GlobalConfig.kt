package io.github.bayang.jelu.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.client.RestClient
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.cfg.CoercionAction
import tools.jackson.databind.cfg.CoercionInputShape
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.type.LogicalType
import java.text.SimpleDateFormat
import java.util.TimeZone

const val SESSION_HEADER_NAME: String = "X-Auth-Token"

@Configuration
class GlobalConfig {
    @Bean
    @Primary
    fun configureJackson(): JsonMapper =
        JsonMapper
            .builder()
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .defaultDateFormat(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"))
            .defaultTimeZone(TimeZone.getDefault())
            .withCoercionConfig(LogicalType.Integer, { it.setAcceptBlankAsEmpty(true) })
            .withCoercionConfig(LogicalType.Integer, { it.setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull) })
            .build()

    @Bean("springRestClient")
    fun springRestClient(): RestClient =
        RestClient
            .builder()
            .configureMessageConverters { converters ->
                converters.registerDefaults().withJsonConverter(
                    JacksonJsonHttpMessageConverter(configureJackson()),
                )
            }.build()

    @Bean("passwordEncoder")
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun corsConfigurationSource(jeluProperties: JeluProperties): UrlBasedCorsConfigurationSource =
        UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration(
                "/**",
                CorsConfiguration().applyPermitDefaultValues().apply {
                    allowedOriginPatterns =
                        if (jeluProperties.cors.allowedOrigins.isNullOrEmpty()) listOf("*") else jeluProperties.cors.allowedOrigins
                    allowedMethods = HttpMethod.values().map { it.name() }
                    allowCredentials = true
                    addAllowedHeader(HttpHeaders.AUTHORIZATION)
                    addExposedHeader(HttpHeaders.CONTENT_DISPOSITION)
                    addExposedHeader(SESSION_HEADER_NAME)
                    addExposedHeader(HttpHeaders.AUTHORIZATION)
                },
            )
        }
}
