package io.github.bayang.jelu.dto
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.properties.Delegates

private val logger = KotlinLogging.logger {}

class MetadataDto {
    var title: String? by Delegates.observable(null) { property, oldValue, newValue ->
        logger.trace { "metadata dto title changed : $oldValue -> $newValue" }
        filled = true
    }
    var isbn10: String? = null
    var isbn13: String? = null
    var summary: String? = null
    var image: String? = null
    var publisher: String? = null
    var pageCount: Int? = null
    var publishedDate: String? = null
    var authors: MutableSet<String> = mutableSetOf()
    var tags: MutableSet<String> = mutableSetOf()
    var series: String? = null
    var numberInSeries: Double? = null
    var language: String? = null
    var googleId: String? = null
    var amazonId: String? = null
    var goodreadsId: String? = null
    var librarythingId: String? = null
    var isfdbId: String? = null
    var openlibraryId: String? = null
    var inventaireId: String? = null
    var noosfereId: String? = null
    var filled: Boolean = false
    var errors: MutableSet<MetadataError> = mutableSetOf()
    var sourcePlugin: String? = null
}

data class MetadataRequestDto(
    val isbn: String? = null,
    val title: String? = null,
    val authors: String? = null,
    val plugins: List<PluginInfo>? = null,
)

data class MetadataError(
    var sourcePlugin: String,
    var errorType: MetadataErrorType,
    var pluginErrorMessage: String? = null,
)
