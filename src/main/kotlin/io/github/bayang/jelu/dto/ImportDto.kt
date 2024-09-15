package io.github.bayang.jelu.dto

import io.github.bayang.jelu.dao.ImportSource

class ImportDto {

    var goodreadsId: String? = null
    var librarythingId: String? = null
    var title: String? = null
    var authors: String? = null
    var isbn10: String? = null
    var isbn13: String? = null
    var publisher: String? = null
    var numberOfPages: Int? = null
    var publishedDate: String? = null
    var readDates: String? = null
    var tags: MutableSet<String> = mutableSetOf()
    var personalNotes: String? = null
    var readCount: Int? = null
    var owned: Boolean? = null
    var importSource: ImportSource? = null
    var review: String? = null
    var rating: Int? = null
}
data class ImportConfigurationDto(
    var shouldFetchMetadata: Boolean,
    var shouldFetchCovers: Boolean,
    var importSource: ImportSource,
)
