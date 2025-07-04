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

        if (doc.title().startsWith("Vyhledávání")) {
            doc.select("p.new a.new").firstOrNull()?.attr("href")?.let { relative ->
                val bookUrl = "https://www.databazeknih.cz$relative"
                val bookDoc = Jsoup.connect(bookUrl).get()
                val dto = parseBookPage(bookDoc)
                val sid = extractSidFromUrl(bookUrl)
                return Pair(dto, sid)
            }
            return Pair(null, null)
        }
        val dto = parseBookPage(doc)
        val sid = extractSidFromDocument(doc)
        return Pair(dto, sid)
    }

    private fun extractSidFromUrl(url: String): String? {
        val regex = Regex("/knihy/(\\d+)-")
        return regex.find(url)?.groupValues?.getOrNull(1)
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

        // 🟢 Fallback: parse .book-biblio block
        if (pageCount == null) {
            doc.extractBiblioField("Počet stran")?.toIntOrNull()?.let { pageCount = it }
        }
        if (language == null) {
            doc.extractBiblioField("Jazyk")?.let { language = mapLanguage(it) }
        }
        if (isbn10.isNullOrBlank() && isbn13.isNullOrBlank()) {
            doc.extractBiblioField("ISBN")?.let { raw ->
                val clean = raw.replace("-", "").replace(" ", "")
                when {
                    clean.length == 10 -> isbn10 = clean
                    clean.length == 13 -> isbn13 = clean
                }
            }
        }

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

    private fun Document.extractBiblioField(label: String): String? {
        val dt = select(".book-biblio dt").firstOrNull { it.text().trim() == label }
        val dd = dt?.nextElementSibling()
        return dd?.text()?.takeIf { it.isNotBlank() }
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
