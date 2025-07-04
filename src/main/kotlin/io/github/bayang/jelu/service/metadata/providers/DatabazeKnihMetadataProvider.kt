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
        config: Map<String, String>,
    ): Optional<MetadataDto> {
        val query = when {
            !metadataRequestDto.isbn.isNullOrBlank() -> metadataRequestDto.isbn!!
            !metadataRequestDto.title.isNullOrBlank() && !metadataRequestDto.authors.isNullOrBlank() ->
                "${metadataRequestDto.title} ${metadataRequestDto.authors}"
            !metadataRequestDto.title.isNullOrBlank() -> metadataRequestDto.title!!
            else -> return Optional.empty()
        }
        val (result, sid) = searchDatabazeKnih(query)

        if (sid != null) {
            fetchIsbnFromDetailPage(sid)?.let { isbn ->
                if (result != null && (result.isbn10.isNullOrBlank() && result.isbn13.isNullOrBlank())) {
                    if (isbn.length == 10) {
                        result.isbn10 = isbn
                    } else if (isbn.length == 13) {
                        result.isbn13 = isbn
                    }
                }
            }
        }

        return if (result != null) Optional.of(result) else Optional.empty()
    }

    private fun searchDatabazeKnih(query: String): Pair<MetadataDto?, String?> {
        val url = "https://www.databazeknih.cz/search?in=books&q=${URLEncoder.encode(query, "UTF-8")}"
        val doc = Jsoup.connect(url).get()

        // If this is a multi-result page, follow the first book link
        if (doc.title().startsWith("Vyhledávání")) {
            doc.select("p.new a.new").firstOrNull()?.attr("href")?.let { relative ->
                val bookUrl = "https://www.databazeknih.cz$relative"
                val canonicalBookUrl = convertToKnihyUrlIfNeeded(bookUrl)
                val bookDoc = Jsoup.connect(canonicalBookUrl).get()
                val dto = parseBookPage(bookDoc)
                val sid = extractSidFromUrl(canonicalBookUrl)
                return Pair(dto, sid)
            }
            return Pair(null, null)
        }

        // On single book page
        val canonicalBookUrl = convertToKnihyUrlIfNeeded(doc.location())
        val canonicalDoc = if (canonicalBookUrl != doc.location()) Jsoup.connect(canonicalBookUrl).get() else doc

        val dto = parseBookPage(canonicalDoc)
        val sid = extractSidFromDocument(canonicalDoc)
        return Pair(dto, sid)
    }

    private fun convertToKnihyUrlIfNeeded(url: String): String {
        val regex = Regex("/(prehled-knihy|knihy)/([\\w-]+)-(\\d+)")
        val match = regex.find(url) ?: return url
        val slug = match.groupValues[2]
        val id = match.groupValues[3]
        return "https://www.databazeknih.cz/knihy/$slug-$id"
    }

    private fun extractSidFromUrl(url: String): String? {
        val regex = Regex("/knihy/(\\d+)-")
        return regex.find(url)?.groupValues?.getOrNull(1)
            ?: Regex("/knihy/[\\w-]+-(\\d+)").find(url)?.groupValues?.getOrNull(1)
    }

    private fun extractSidFromDocument(doc: Document): String? {
        doc.select("link[rel=canonical]").firstOrNull()?.attr("href")?.let { canonical ->
            val regex = Regex("/knihy/(\\d+)-")
            return regex.find(canonical)?.groupValues?.getOrNull(1)
        }
        doc.select("[data-sid]").firstOrNull()?.attr("data-sid")?.let {
            if (it.isNotBlank()) return it
        }
        return null
    }

    private fun fetchIsbnFromDetailPage(sid: String): String? {
        val detailUrl = "https://www.databazeknih.cz/book-detail-more-info/$sid"
        val doc = Jsoup.connect(detailUrl).get()
        return doc.select("[itemprop=isbn]").firstOrNull()?.text()?.replace("-", "")?.replace(" ", "")
    }

    private fun parseBookPage(doc: Document): MetadataDto? {
        val dto = MetadataDto()

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

        doc.selectFirst("h3 > a[href^=/serie/]")?.let { serieLink ->
            series = serieLink.text().trim()
            doc.selectFirst("span.nowrap > span.odright_pet, span.nowrap > span.odleft_pet")?.text()?.let { nstr ->
                val num = nstr.removeSuffix(". díl").trim()
                numberInSeries = num.toDoubleOrNull()
            }
        }

        tags.addAll(doc.select("a.tag").map(Element::text))

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

    private fun mapLanguage(dbLang: String): String? = when (dbLang.lowercase()) {
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
