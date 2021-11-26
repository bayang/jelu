package io.github.bayang.jelu.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotBlank

@ConfigurationProperties(prefix = "jelu")
@ConstructorBinding
@Validated
data class JeluProperties(val database: Database, val files: Files) {

    data class Database(
        @get:NotBlank var path: String
        )

    data class Files(
        @get:NotBlank var dir: String
    )
}