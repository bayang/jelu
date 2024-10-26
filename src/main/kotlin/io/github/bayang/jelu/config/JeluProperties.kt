package io.github.bayang.jelu.config

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "jelu")
@Validated
data class JeluProperties(
    val database: Database,
    val files: Files,
    val session: Session,
    val cors: Cors = Cors(),
    val metadata: Metadata = Metadata(Calibre(null)),
    val auth: Auth = Auth(
        Ldap(),
        Proxy(),
    ),
    val metadataProviders: List<MetaDataProvider>?,
    val lucene: Lucene = Lucene(indexAnalyzer = IndexAnalyzer()),
) {

    data class MetaDataProvider(
        var name: String,
        var isEnabled: Boolean = false,
        var apiKey: String?,
        var order: Int = -1000,
    )

    data class Database(
        @get:NotBlank var path: String,
    )

    data class Files(
        @get:NotBlank var images: String,
        @get:NotBlank var imports: String,
        var resizeImages: Boolean = true,
    )

    data class Session(
        @get:Positive var duration: Long,
    )

    data class Cors(
        var allowedOrigins: List<String> = emptyList(),
    )

    data class Calibre(
        var path: String?,
        var order: Int = 1000,
    )

    data class Metadata(
        var calibre: Calibre,
    )

    data class Auth(
        var ldap: Ldap,
        var proxy: Proxy,
    )

    data class Ldap(
        var enabled: Boolean = false,
        val url: String = "",
        val userDnPatterns: List<String> = emptyList(),
        val userSearchFilter: String = "",
        val userSearchBase: String = "",
        val userDn: String = "",
        val password: String = "",
    )

    data class Proxy(
        var enabled: Boolean = false,
        val adminName: String = "",
        val header: String = "X-Authenticated-User",
    )

    data class IndexAnalyzer(
        @get:Positive
        var minGram: Int = 3,
        @get:Positive
        var maxGram: Int = 10,
        var preserveOriginal: Boolean = true,
    )

    data class Lucene(
        @get:NotBlank
        var dataDirectory: String = "",

        var indexAnalyzer: IndexAnalyzer,

    )
}
