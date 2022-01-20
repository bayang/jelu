package io.github.bayang.jelu.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Positive

@ConfigurationProperties(prefix = "jelu")
@ConstructorBinding
@Validated
data class JeluProperties(
    val database: Database,
    val files: Files,
    val session: Session,
    val cors: Cors = Cors(),
    val metadata: Metadata = Metadata(Calibre(null))
) {

    data class Database(
        @get:NotBlank var path: String
    )

    data class Files(
        @get:NotBlank var dir: String
    )

    data class Session(
        @get:Positive var duration: Int
    )

    data class Cors(
        var allowedOrigins: List<String> = emptyList()
    )

    data class Calibre(
        var path: String?
    )

    data class Metadata(
        var calibre: Calibre
    )
}
