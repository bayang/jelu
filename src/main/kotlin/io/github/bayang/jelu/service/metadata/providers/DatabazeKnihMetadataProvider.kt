package io.github.bayang.jelu.service.metadata.providers

import io.github.bayang.jelu.dto.MetadataDto
import io.github.bayang.jelu.dto.MetadataRequestDto
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.util.Optional

@Service
class DatabazeKnihMetadataProvider : IMetaDataProvider {

    override fun name(): String = "databazeknih"

    override fun fetchMetadata(
        metadataRequestDto: MetadataRequestDto,
        config: Map<String, String>
    ): Optional<MetadataDto> {
        val query = when {
            !metadataRequestDto.isbn.isNullOrBlank() -> metadataRequestDto.isbn!!
            !metadataRequestDto.title.isNullOrBlank() && !metadataRequestDto.authors.isNullOrBlank() ->
                "${metadataRequestDto.title} ${metadataRequestDto.authors}"
            !metadataRequestDto.title.isNullOrBlank() -> metadataRequestDto.title!!
            else -> return Optional.empty()
        }
        val result = searchDatabazeKnih(query)
        return if (result != null) Optional.of(result) else Optional.empty()
    }

    private fun searchDatabazeKnih(query: String): MetadataDto? {
        val url = "https://www.databazeknih.cz/search?in=books&q=${URLEncoder.encode(query, "UTF-8")}"
        val doc = Jsoup.connect(url).get()

        // If this is a multi-result page, follow the first book link
        if (doc.title().startsWith("Vyhledávání")) {
            doc.select("p.new a.new").firstOrNull()?.attr("href")?.let { relative ->
                val bookUrl = "https://www.databazeknih.cz$relative"
                return parseBookPage(Jsoup.connect(bookUrl).get())
            }
            return null
        }
        return parseBookPage(doc)
    }

    private fun parseBookPage(doc: Document): MetadataDto? {
        val dto = MetadataDto()
        // Title & Image
        doc.head().select("meta").forEach { metaTag ->
            when (metaTag.attr("property")) {
                "og:title" -> dto.title = metaTag.attr("content")
                "og:image" -> dto.image = metaTag.attr("content")
            }
        }

        val authors = mutableSetOf<String>()
        val tags = mutableSetOf<String>()
        var publisher: String? = null
        var summary: String? = null
        var language: String? = null
        var pageCount: Int? = null
        var publishedDate: String? = null
        var series: String? = null
        var numberInSeries: Double? = null
        var isbn10: String? = null
        var isbn13: String? = null

        // Main [itemprop] fields
        for (item in doc.select("[itemprop]")) {
            when (item.attr("itemprop")) {
                "author" -> {
                    item.select("a").forEach { a ->
                        val authorName = a.text().trim()
                        if (authorName.isNotEmpty()) authors.add(authorName)
                    }
                }
                "description" -> {
                    val desc = item.nextElementSibling()?.wholeText()?.trim()
                    if (!desc.isNullOrBlank() && desc != "Popis knihy zde zatím bohužel není...") {
                        summary = desc.split("\n")
                            .map { it.trim() }
                            .filter { it != "... celý text" }
                            .joinToString("\n")
                            .removeSuffix("... celý text")
                            .trim()
                    }
                }
                "publisher" -> publisher = item.text()
                "genre" -> tags.addAll(item.select("a.genre").map(Element::text))
                "datePublished" -> {
                    val t = item.text()
                    if (t.isNotEmpty() && t != "?") publishedDate = t
                }
                "isbn" -> {
                    val raw = item.text().replace("-", "").replace(" ", "")
                    when {
                        raw.length == 10 -> isbn10 = raw
                        raw.length == 13 -> isbn13 = raw
                    }
                }
                "language" -> language = mapLanguage(item.text())
                "numberOfPages" -> item.text().toIntOrNull()?.let { pageCount = it }
            }
        }

        // Series
        doc.selectFirst("h3 > a[href^=/serie/]")?.let { serieLink ->
            series = serieLink.text().trim()
            doc.selectFirst("span.nowrap > span.odright_pet, span.nowrap > span.odleft_pet")?.text()?.let { nstr ->
                val num = nstr.removeSuffix(". díl").trim()
                numberInSeries = num.toDoubleOrNull()
            }
        }

        // Add tags (from <a class="tag">)
        tags.addAll(doc.select("a.tag").map(Element::text))

        // Populate DTO
        dto.title = dto.title ?: ""
        dto.authors = authors
        dto.tags = tags
        dto.publisher = publisher
        dto.summary = summary
        dto.language = language
        dto.pageCount = pageCount
        dto.publishedDate = publishedDate
        dto.series = series
        dto.numberInSeries = numberInSeries
        dto.isbn10 = isbn10
        dto.isbn13 = isbn13

        return if (dto.title.isNullOrBlank()) null else dto
    }

    private fun mapLanguage(dbLang: String): String? = when (dbLang) {
        "český" -> "ces"
        "slovenský" -> "slo"
        "německý" -> "deu"
        "polský" -> "pol"
        "anglický" -> "eng"
        "francouzský" -> "fre"
        "španělský" -> "spa"
        "italský" -> "ita"
        else -> dbLang
    }
}
