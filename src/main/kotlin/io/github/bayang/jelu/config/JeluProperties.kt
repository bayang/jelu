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
    val metadata: Metadata = Metadata(Calibre(null)),
    val auth: Auth = Auth(
        Ldap(),
        Proxy()
    ),
    val metadataProviders: List<MetaDataProvider>?,
) {

    data class MetaDataProvider(
        var name: String,
        var isEnabled: Boolean = false,
        var apiKey: String?,
        var order: Int = -1000
    )

    data class Database(
        @get:NotBlank var path: String
    )

    data class Files(
        @get:NotBlank var images: String,
        @get:NotBlank var imports: String,
        var resizeImages: Boolean = true
    )

    data class Session(
        @get:Positive var duration: Int
    )

    data class Cors(
        var allowedOrigins: List<String> = emptyList()
    )

    data class Calibre(
        var path: String?,
        var order: Int = 1000
    )

    data class Metadata(
        var calibre: Calibre
    )

    data class Auth(
        var ldap: Ldap,
        var proxy: Proxy
    )

    data class Ldap(
        var enabled: Boolean = false,
        val url: String = "",
        val userDnPatterns: List<String> = emptyList(),
        val userSearchFilter: String = "",
        val userSearchBase: String = ""
    )

    data class Proxy(
        var enabled: Boolean = false,
        val adminName: String = "",
        val header: String = "X-Authenticated-User",
    )
}
