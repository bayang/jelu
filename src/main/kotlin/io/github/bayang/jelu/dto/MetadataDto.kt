package io.github.bayang.jelu.dto

data class MetadataDto
    (var title: String? = null,
    var isbn10:String? = null,
    var isbn13: String? = null,
    var summary: String? = null,
    var image: String? = null,
    var publisher: String? = null,
    var pageCount: Int? = null,
    var publishedDate: String? = null,
    var authors: MutableList<String> = mutableListOf(),
    var tags: MutableList<String> = mutableListOf(),
    var series: String? = null,
    var numberInSeries: Double? = null,
    var googleId: String? = null,
    var amazonId: String? = null,
    var goodreadsId: String? = null,
)