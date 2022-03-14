package io.github.bayang.jelu.dto

import com.fasterxml.jackson.annotation.JsonAlias

data class WikipediaPageResult(
    val type: String,
    val title: String,
    @JsonAlias("displaytitle")
    val displayTitle: String?,
    @JsonAlias("wikibase_item")
    val wikibaseItem: String?,
    @JsonAlias("pageid")
    val pageId: Long,
    val lang: String?,
    val description: String?,
    val extract: String,
    @JsonAlias("extract_html")
    val extractHtml: String,
    @JsonAlias("content_urls")
    val contentUrls: ContentUrlList,
    val thumbnail: PageThumbnail?,
    @JsonAlias("originalimage")
    val originalImage: PageThumbnail?
)

data class PageThumbnail(
    val source: String,
    val width: Int,
    val height: Int
)

data class ContentUrlList(
    val desktop: ContentUrl,
    val mobile: ContentUrl
)

data class ContentUrl(
    val page: String,
    val revisions: String,
    val edit: String,
    val talk: String
)
