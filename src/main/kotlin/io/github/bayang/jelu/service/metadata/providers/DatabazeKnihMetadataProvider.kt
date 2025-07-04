package io.github.bayang.jelu.service.metadata.providers

import io.github.bayang.jelu.dto.MetadataDto
import io.github.bayang.jelu.dto.MetadataRequestDto
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.util.Optional

@Service
class DatabazeKnihMetadataProvider : IMetaDataProvider {

    private val logger = LoggerFactory.getLogger(DatabazeKnihMetadataProvider::class.java)

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
            else -> {
                logger.debug("No valid query for DatabazeKnih")
                return Optional.empty()
            }
        }

        logger.debug("Searching DatabazeKnih with query: $query")
        val (result, sid) = searchDatabazeKnih(query)

        if (sid != null) {
            logger.debug("SID found: $sid, fetching extended details")
            fetchIsbnLanguagePageCount(sid, result)
        } else {
            logger.debug("SID not found, skipping extended details fetch")
        }

        return if (result != null) Optional.of(result) else Optional.empty()
    }

    /**
     * Returns Pair<MetadataDto?, SID?>
     */
    private fun searchDatabazeKnih(query: String): Pair<MetadataDto?, String?> {
        val url = "https://www.databazeknih.cz/search?in=books&q=${URLEncoder.encode(query, "UTF-8")}"
        logger.debug("Fetching search URL: $url")
        val doc = Jsoup.connect(url).get()

        logger.debug("Search page title: ${doc.title()}")

        // If this is a multi-result page, follow the first book link
        if (doc.title().startsWith("Vyhledávání")) {
            val firstBookLink = doc.select("p.new a.new").firstOrNull()?.attr("href")
            if (firstBookLink != null) {
                val bookUrl = "https://www.databazeknih.cz$firstBookLink"
                logger.debug("Multi-result page detected, fetching first book page: $bookUrl")
                val bookDoc = Jsoup.connect(bookUrl).get()
                val dto = parseBookPage(bookDoc)
                val sid = extractSidFromUrl(bookUrl)
                return Pair(dto, sid)
            }
            return Pair(null, null)
        }

        // On single book page
        val dto = parseBookPage(doc)
        val sid = extractSidFromDocument(doc)
        return Pair(dto, sid)
    }

    private fun extractSidFromUrl(url: String): String? {
        val regex = Regex("/knihy/(\\d+)-")
        return regex.find(url)?.groupValues?.getOrNull(1)
    }

    private fun extractSidFromDocument(doc: Document): String? {
        // Try canonical link
        doc.select("link[rel=canonical]").firstOrNull()?.attr("href")?.let { canonical ->
            val regex = Regex("/knihy/(\\d+)-")
            regex.find(canonical)?.groupValues?.getOrNull(1)?.let {
                logger.debug("SID extracted from canonical link: $it")
                return it
            }
        }
        // Try data-sid attribute
        doc.select("[data-sid]").firstOrNull()?.attr("data-sid")?.let {
            if (it.isNotBlank()) {
                logger.debug("SID extracted from data-sid attribute: $it")
                return it
            }
        }
        // Try span#moreBookDetails bookid attribute
        doc.select("span#moreBookDetails").firstOrNull()?.attr("bookid")?.let {
            if (it.isNotBlank()) {
                logger.debug("SID extracted from span#moreBookDetails bookid: $it")
                return it
            }
        }
        logger.debug("SID not found in document")
        return null
    }

    /**
     * Fetch ISBN, language, and page count from extended detail page.
     */
    private fun fetchIsbnLanguagePageCount(sid: String, dto: MetadataDto?) {
        if (dto == null) return
        try {
            val detailUrl = "https://www.databazeknih.cz/book-detail-more-info/$sid"
            logger.debug("Fetching extended details from: $detailUrl")
            val doc = Jsoup.connect(detailUrl).get()

            // ISBN
            val isbnRaw = doc.select("[itemprop=isbn]").firstOrNull()?.text()?.replace("-", "")?.replace(" ", "")
            if (!isbnRaw.isNullOrBlank()) {
                if (isbnRaw.length == 10 && dto.isbn10.isNullOrBlank()) {
                    dto.isbn10 = isbnRaw
                    logger.debug("ISBN-10 found: $isbnRaw")
                } else if (isbnRaw.length == 13 && dto.isbn13.isNullOrBlank()) {
                    dto.isbn13 = isbnRaw
                    logger.debug("ISBN-13 found: $isbnRaw")
                }
            } else {
                logger.debug("ISBN not found in extended details")
            }

            // Language
            val langRaw = doc.select("[itemprop=language]").firstOrNull()?.text()
            if (!langRaw.isNullOrBlank()) {
                dto.language = mapLanguage(langRaw.trim())
                logger.debug("Language found: ${dto.language} (raw: $langRaw)")
            } else {
                logger.debug("Language not found in extended details")
            }

            // Page Count
            val pagesText = doc.select("[itemprop=numberOfPages]").firstOrNull()?.text()
            pagesText?.toIntOrNull()?.let {
                dto.pageCount = it
                logger.debug("Page count found: $it")
            } ?: logger.debug("Page count not found or invalid in extended details")

        } catch (e: Exception) {
            logger.error("Error fetching extended details for SID $sid: ${e.message}", e)
        }
    }

    private fun parseBookPage(doc: Document): MetadataDto? {
        logger.debug("parseBookPage started")
        val dto = MetadataDto()

        // Title and image from meta tags
        doc.head().select("meta").forEach { metaTag ->
            when (metaTag.attr("property")) {
                "og:title" -> {
                    dto.title = metaTag.attr("content")
                    logger.debug("Title extracted: ${dto.title}")
                }
                "og:image" -> {
                    dto.image = metaTag.attr("content")
                    logger.debug("Image extracted from meta: ${dto.image}")
                }
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

        // Authors, description, publisher, tags, date, isbn, language, page count
        for (item in doc.select("[itemprop]")) {
            when (item.attr("itemprop")) {
                "author" -> {
                    item.select("a").forEach { a ->
                        val authorName = a.text().trim()
                        if (authorName.isNotEmpty()) authors.add(authorName)
                    }
                    logger.debug("Authors extracted: $authors")
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
                        logger.debug("Summary extracted")
                    }
                }
                "publisher" -> {
                    publisher = item.text()
                    logger.debug("Publisher extracted: $publisher")
                }
                "genre" -> {
                    tags.addAll(item.select("a.genre").map(Element::text))
                    logger.debug("Tags extracted: $tags")
                }
                "datePublished" -> {
                    val t = item.text()
                    if (t.isNotEmpty() && t != "?") {
                        publishedDate = t
                        logger.debug("Published date extracted: $publishedDate")
                    }
                }
                "isbn" -> {
                    val raw = item.text().replace("-", "").replace(" ", "")
                    when {
                        raw.length == 10 -> isbn10 = raw
                        raw.length == 13 -> isbn13 = raw
                    }
                }
                "language" -> {
                    language = mapLanguage(item.text())
                }
                "numberOfPages" -> {
                    item.text().toIntOrNull()?.let {
                        pageCount = it
                        logger.debug("Page count extracted: $pageCount")
                    }
                }
            }
        }

        // Fallbacks for page count and language from labels in table cells
        if (pageCount == null) {
            doc.extractField("Počet stran")?.toIntOrNull()?.let {
                pageCount = it
                logger.debug("Page count extracted from fallback: $pageCount")
            }
        }
        if (language == null) {
            doc.extractField("Jazyk vydání")?.let {
                language = mapLanguage(it)
                logger.debug("Language extracted from fallback: $language")
            }
        }

        // Series info
        doc.selectFirst("h3 > a[href^=/serie/]")?.let { serieLink ->
            series = serieLink.text().trim()
            doc.selectFirst("span.nowrap > span.odright_pet, span.nowrap > span.odleft_pet")?.text()?.let { nstr ->
                val num = nstr.removeSuffix(". díl").trim()
                numberInSeries = num.toDoubleOrNull()
            }
        }

        tags.addAll(doc.select("a.tag").map(Element::text))

        // Try extracting cover image if not already set
        if (dto.image.isNullOrBlank()) {
            val coverImg = doc.select("div.book-cover img").firstOrNull()?.attr("src")
            if (!coverImg.isNullOrBlank()) {
                dto.image = coverImg
                logger.debug("Cover image extracted from img tag: ${dto.image}")
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

        if (dto.title.isNullOrBlank()) {
            logger.debug("No title found, returning null")
            return null
        }

        logger.debug("Returning metadata: $dto")
        return dto
    }

    private fun mapLanguage(dbLang: String): String? = when (dbLang.lowercase()) {
        "český", "česká" -> "ces"
        "slovenský", "slovenská" -> "slo"
        "německý" -> "deu"
        "polský" -> "pol"
        "anglický" -> "eng"
        "francouzský" -> "fre"
        "španělský" -> "spa"
        "italský" -> "ita"
        else -> dbLang
    }

    /**
     * Helper to extract a detail field by label from <td>.
     */
    private fun Document.extractField(label: String): String? {
        return select("td:containsOwn($label)").next().text().takeIf { it.isNotBlank() }
    }
}
