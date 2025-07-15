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

    /**
     * Entry point for fetching metadata from databazeknih.cz
     */
    override fun fetchMetadata(
        metadataRequestDto: MetadataRequestDto,
        config: Map<String, String>,
    ): Optional<MetadataDto> {
        // Build search query from ISBN or title+author or just title
        val query = when {
            !metadataRequestDto.isbn.isNullOrBlank() -> metadataRequestDto.isbn
            !metadataRequestDto.title.isNullOrBlank() && !metadataRequestDto.authors.isNullOrBlank() ->
                "${metadataRequestDto.title} ${metadataRequestDto.authors}"
            !metadataRequestDto.title.isNullOrBlank() -> metadataRequestDto.title
            else -> {
                logger.debug("No valid query for DatabazeKnih")
                return Optional.empty()
            }
        }

        logger.debug("Searching DatabazeKnih with query: \"$query\"")
        val (result, sid) = searchDatabazeKnih(query)

        // If SID and result found, fetch extended details like ISBN, language, page count
        if (sid != null && result != null) {
            logger.debug("SID found: $sid, fetching extended details for \"${result.title}\"")
            fetchExtendedDetails(sid, result)
        }

        return result?.let { Optional.of(it) } ?: Optional.empty()
    }

    /**
     * Search databazeknih.cz and return metadata DTO + book SID
     */
    private fun searchDatabazeKnih(query: String): Pair<MetadataDto?, String?> {
        val url = "https://www.databazeknih.cz/search?in=books&q=${URLEncoder.encode(query, "UTF-8")}"
        logger.debug("Fetching search URL: $url")
        val doc = fetchDocument(url) ?: return Pair(null, null)

        logger.debug("Search page title: ${doc.title()}")

        // If search results page, go to first book detail page
        if (doc.title().startsWith("Vyhledávání")) {
            val firstBookLink = doc.select("p.new a.new").firstOrNull()?.attr("href")
            if (firstBookLink != null) {
                val bookUrl = "https://www.databazeknih.cz$firstBookLink"
                logger.debug("Multi-result page detected, fetching first book page: $bookUrl")
                val bookDoc = fetchDocument(bookUrl) ?: return Pair(null, null)
                val dto = parseBookPage(bookDoc)
                val sid = extractSidFromUrl(bookUrl)
                return Pair(dto, sid)
            }
            return Pair(null, null)
        }

        // Direct hit
        val dto = parseBookPage(doc)
        val sid = extractSidFromDocument(doc)
        return Pair(dto, sid)
    }

    /**
     * Extract SID from book detail page URL
     */
    private fun extractSidFromUrl(url: String): String? {
        val regex = Regex("/(knihy|prehled-knihy)/[a-z0-9\\-]+-(\\d+)")
        return regex.find(url)?.groupValues?.getOrNull(2)
    }

    /**
     * Extract SID from book detail page HTML
     */
    private fun extractSidFromDocument(doc: Document): String? {
        doc.select("link[rel=canonical]").firstOrNull()?.attr("href")?.let { canonical ->
            val regex = Regex("/knihy/(\\d+)-")
            regex.find(canonical)?.groupValues?.getOrNull(1)?.let {
                logger.debug("SID extracted from canonical link: $it")
                return it
            }
        }
        doc.select("[data-sid]").firstOrNull()?.attr("data-sid")?.takeIf { it.isNotBlank() }?.let {
            logger.debug("SID extracted from data-sid attribute: $it")
            return it
        }
        doc.select("span#moreBookDetails").firstOrNull()?.attr("bookid")?.takeIf { it.isNotBlank() }?.let {
            logger.debug("SID extracted from span#moreBookDetails bookid: $it")
            return it
        }
        logger.debug("SID not found in document")
        return null
    }

    /**
     * Fetch and populate extended book details (fallbacks included)
     */
    private fun fetchExtendedDetails(sid: String, dto: MetadataDto) {
        val detailUrl = "https://www.databazeknih.cz/book-detail-more-info/$sid"
        logger.debug("Fetching extended details from: $detailUrl")
        val doc = fetchDocument(detailUrl) ?: return

        // ISBN (with safer fallback, fix two ISBNs)
        val isbnText = doc.select("[itemprop=isbn]").firstOrNull()?.text()?.takeIf { it.isNotBlank() }
            ?: doc.select("div.book-info").text()
                .substringAfter("ISBN:")
                .substringBefore("Vazba:") // stop before next field if applicable
                .trim()

        if (!isbnText.isNullOrBlank()) {
            val isbnCandidates = isbnText.split(",").map { it.trim() }

            var isbn10: String? = null
            var isbn13: String? = null

            isbnCandidates.forEach { raw ->
                val cleaned = raw.replace("-", "").replace(" ", "")
                when (cleaned.length) {
                    10 -> if (isbn10 == null) isbn10 = cleaned
                    13 -> if (isbn13 == null) isbn13 = cleaned
                    7 -> if (isbn10 == null) isbn10 = cleaned
                }
            }

            if (dto.isbn10.isNullOrBlank() && isbn10 != null) dto.isbn10 = isbn10
            if (dto.isbn13.isNullOrBlank() && isbn13 != null) dto.isbn13 = isbn13

            logger.debug("Extended ISBN detected: ISBN-10=${dto.isbn10}, ISBN-13=${dto.isbn13}")
        }

        // Language (with fallback)
        doc.select("[itemprop=language]").firstOrNull()?.text()?.trim()?.let {
            dto.language = mapLanguage(it)
            logger.debug("Extended language detected (itemprop): ${dto.language}")
        } ?: run {
            val fallbackLang = doc.select("div.book-info").text().substringAfter("Jazyk:").substringBefore(" ").trim()
            if (fallbackLang.isNotEmpty()) {
                dto.language = mapLanguage(fallbackLang)
                logger.debug("Extended language detected (fallback): ${dto.language}")
            }
        }

        // Page count (with fallback)
        doc.select("[itemprop=numberOfPages]").firstOrNull()?.text()?.toIntOrNull()?.let {
            dto.pageCount = it
            logger.debug("Extended page count detected (itemprop): $it")
        } ?: run {
            val fallbackPage = doc.select("div.book-info").text().substringAfter("Počet stran:").substringBefore(" ").trim()
            fallbackPage.toIntOrNull()?.let {
                dto.pageCount = it
                logger.debug("Extended page count detected (fallback): $it")
            }
        }
    }

    /**
     * Parse book detail page into MetadataDto (basic info only)
     */
    private fun parseBookPage(doc: Document): MetadataDto? {
        val dto = MetadataDto()

        // Basic metadata
        doc.select("meta[property=og:title]").firstOrNull()?.attr("content")?.let { dto.title = it }
        doc.select("meta[property=og:image]").firstOrNull()?.attr("content")?.let { dto.image = it }
        doc.select("#icover_mid img.kniha_img.coverOnDetail").firstOrNull()?.attr("src")?.let { dto.image = it }

        val authors = mutableSetOf<String>()
        val tags = mutableSetOf<String>()

        // Parse schema.org itemprops
        for (item in doc.select("[itemprop]")) {
            when (item.attr("itemprop")) {
                "author" -> item.select("a").forEach { authors.add(it.text().trim()) }
                "description" -> {
                    val desc = item.nextElementSibling()?.wholeText()?.trim()
                    if (!desc.isNullOrBlank() && desc != "Popis knihy zde zatím bohužel není...") {
                        dto.summary = desc.split("\n").map { it.trim() }
                            .filterNot { it == "... celý text" }
                            .joinToString("\n").removeSuffix("... celý text").trim()
                    }
                }
                "publisher" -> dto.publisher = item.text()
                "genre" -> tags.addAll(item.select("a.genre").map(Element::text))
                "datePublished" -> item.text().takeIf { it.isNotEmpty() && it != "?" }?.let { dto.publishedDate = it }
                "isbn" -> {
                    val (isbn10, isbn13) = parseIsbn(item.text())
                    dto.isbn10 = isbn10
                    dto.isbn13 = isbn13
                }
                "language" -> dto.language = mapLanguage(item.text())
                "numberOfPages" -> item.text().toIntOrNull()?.let { dto.pageCount = it }
            }
        }

        // Tags and authors
        tags.addAll(doc.select("a.tag").map(Element::text))
        dto.authors = authors
        dto.tags = tags

        // Ensure we have a title
        val title = dto.title ?: ""
        dto.title = title

        if (title.isBlank()) {
            logger.debug("No title found, returning null")
            return null
        }

        return dto
    }

    /**
     * Parses raw ISBN string and returns pair of ISBN10 and ISBN13.
     * Also treats 7-digit older ISBN as ISBN10.
     */
    private fun parseIsbn(raw: String): Pair<String?, String?> {
        val cleaned = raw.replace("-", "").replace(" ", "")
        return when (cleaned.length) {
            7 -> Pair(cleaned, null) // Old style ISBN treated as ISBN10
            10 -> Pair(cleaned, null)
            13 -> Pair(null, cleaned)
            else -> Pair(null, null)
        }
    }

    // Map Czech language names into ISO 639 codes
    private val languageMap = mapOf(
        "český" to "ces", "česká" to "ces",
        "slovenský" to "slo", "slovenská" to "slo",
        "německý" to "deu", "polský" to "pol",
        "anglický" to "eng", "francouzský" to "fre",
        "španělský" to "spa", "italský" to "ita",
    )

    private fun mapLanguage(dbLang: String): String? =
        languageMap[dbLang.lowercase()] ?: dbLang

    /**
     * Utility: Fetch HTML document with retries
     */
    private fun fetchDocument(url: String, retries: Int = 3): Document? {
        repeat(retries - 1) {
            try {
                return Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; JeluBot/1.0; +https://github.com/bayang/jelu)")
                    .timeout(10_000)
                    .followRedirects(true)
                    .get()
            } catch (e: Exception) {
                logger.warn("Attempt ${it + 1} failed for $url: ${e.message}")
            }
        }
        return try {
            Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (compatible; JeluBot/1.0; +https://github.com/bayang/jelu)")
                .timeout(10_000)
                .followRedirects(true)
                .get()
        } catch (e: Exception) {
            logger.error("Final attempt failed for $url: ${e.message}", e)
            null
        }
    }
}
