package io.github.bayang.jelu.dto

import com.fasterxml.jackson.annotation.JsonAlias

data class WikipediaSearchResultElement(

    val id: Long,
    val key: String,
    val title: String,
    val excerpt: String,
    val description: String?,
    @JsonAlias("matched_title")
    val matchedTitle: String?,
    val thumbnail: Thumbnail?
)

data class Thumbnail(
    val mimetype: String,
    val size: String?,
    val width: Int,
    val height: Int,
    val duration: Int,
    val url: String
)

data class WikipediaSearchResult(
    val pages: List<WikipediaSearchResultElement>
)
