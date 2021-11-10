package io.github.bayang.jelu.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotBlank

@ConfigurationProperties(prefix = "jelu")
@ConstructorBinding
@Validated
data class JeluProperties(val database: Database) {

    data class Database(
        @get:NotBlank var path: String
        )
}