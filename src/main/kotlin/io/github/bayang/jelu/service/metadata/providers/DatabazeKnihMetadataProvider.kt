package io.github.bayang.jelu.service.metadata.providers

import io.github.bayang.jelu.dto.MetadataDto
import io.github.bayang.jelu.dto.MetadataRequestDto
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
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
        // Build search query using ISBN, title + author, or just title
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

        // Perform search and extract basic book data + SID (book id)
        val (result, sid) = searchDatabazeKnih(query)

        // If SID is found, fetch extended details (pages, ISBN, language, etc.)
        if (sid != null && result != null) {
            logger.debug("SID found: $sid, fetching extended details for \"${result.title}\"")
            fetchExtendedDetails(sid, result, metadataRequestDto.isbn)
        }

        return result?.let { Optional.of(it) } ?: Optional.empty()
    }

    /**
     * Searches DatabazeKnih using the query.
     * Returns a MetadataDto (basic book info) and the book's SID if found.
     */
    private fun searchDatabazeKnih(query: String): Pair<MetadataDto?, String?> {
        val url = "https://www.databazeknih.cz/search?in=books&q=${URLEncoder.encode(query, "UTF-8")}"
        logger.debug("Fetching search URL: $url")

        val doc = fetchDocument(url) ?: return Pair(null, null)

        // If search result page lists multiple books, open the first book's detail page
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

        // If the search goes directly to the book page
        val dto = parseBookPage(doc)
        val sid = extractSidFromDocument(doc)
        return Pair(dto, sid)
    }

    /**
     * Extract SID from URL using regex.
     */
    private fun extractSidFromUrl(url: String): String? {
        val regex = Regex("/(knihy|prehled-knihy)/[a-z0-9\\-]+-(\\d+)")
        return regex.find(url)?.groupValues?.getOrNull(2)
    }

    /** Extract SID from document's canonical link or attributes */
    private fun extractSidFromDocument(doc: Document): String? {
        // Try canonical link
        doc.select("link[rel=canonical]").firstOrNull()?.attr("href")?.let { canonical ->
            val regex = Regex("/(knihy|prehled-knihy)/[a-z0-9\\-]+-(\\d+)")
            regex.find(canonical)?.groupValues?.getOrNull(2)?.let {
                logger.debug("SID extracted from canonical link: $it")
                return it
            }
        }
        // Try data-sid attribute
        doc.select("[data-sid]").firstOrNull()?.attr("data-sid")?.takeIf { it.isNotBlank() }?.let {
            logger.debug("SID extracted from data-sid attribute: $it")
            return it
        }
        // Try #moreBookDetails bookid
        doc.select("span#moreBookDetails").firstOrNull()?.attr("bookid")?.takeIf { it.isNotBlank() }?.let {
            logger.debug("SID extracted from span#moreBookDetails bookid: $it")
            return it
        }
        return null
    }

    /**
     * Fetch additional info like ISBN, language, and page count from book-detail-more-info endpoint
     */
    private fun fetchExtendedDetails(sid: String, dto: MetadataDto, searchedIsbn: String?) {
        val detailUrl = "https://www.databazeknih.cz/book-detail-more-info/$sid"
        logger.debug("Fetching extended details from: $detailUrl")
        val doc = fetchDocument(detailUrl) ?: return

        // ISBN parsing (can be multiple ISBNs)
        val isbnElements = doc.select("span.category:contains(ISBN:) + span, [itemprop=isbn]")
        val isbnText = isbnElements.joinToString(", ") { it.text().trim() }
        var foundMatch = false

        if (isbnText.isNotBlank()) {
            val isbnCandidates = isbnText.split(",").map { it.trim() }
            var isbn10: String? = null
            var isbn13: String? = null

            isbnCandidates.forEach { raw ->
                val cleaned = raw.replace("-", "").replace(" ", "")

                // Check if searched ISBN matches any candidate
                if (searchedIsbn != null && cleaned.equals(
                        searchedIsbn.replace("-", "").replace(" ", ""), true,
                    )
                ) {
                    foundMatch = true
                }

                // Store first valid ISBN10/ISBN13 found
                when (cleaned.length) {
                    7, 8, 10 -> if (isbn10 == null) isbn10 = cleaned
                    13 -> if (isbn13 == null) isbn13 = cleaned
                }
            }

            if (dto.isbn10.isNullOrBlank() && isbn10 != null) dto.isbn10 = isbn10
            if (dto.isbn13.isNullOrBlank() && isbn13 != null) dto.isbn13 = isbn13
        }

        // If searched ISBN not matched, fallback and store it manually
        if (searchedIsbn != null && !foundMatch) {
            val cleaned = searchedIsbn.replace("-", "").replace(" ", "")
            when (cleaned.length) {
                13 -> if (dto.isbn13.isNullOrBlank()) dto.isbn13 = cleaned
                7, 8, 10 -> if (dto.isbn10.isNullOrBlank()) dto.isbn10 = cleaned
            }
        }

        // Language
        val langElement = doc.select("[itemprop=language]").firstOrNull()
            ?: doc.select("span.category:contains(Jazyk vydání:) + span").firstOrNull()
        langElement?.text()?.trim()?.let {
            dto.language = mapLanguage(it)
        }

        // Page count
        doc.select("[itemprop=numberOfPages]").firstOrNull()?.text()?.toIntOrNull()?.let {
            dto.pageCount = it
        } ?: run {
            val fallbackPage = doc.select("span.category:contains(Počet stran:) + span").firstOrNull()?.text()?.trim()
            fallbackPage?.toIntOrNull()?.let {
                dto.pageCount = it
            }
        }
    }

    /**
     * Parse book page (basic metadata: title, image, authors, description, publisher, etc.)
     */
    private fun parseBookPage(doc: Document): MetadataDto? {
        val dto = MetadataDto()

        // Title
        doc.select("meta[property=og:title]").firstOrNull()?.attr("content")?.let { dto.title = it }

        // Image (prefer inline high-res cover image if available)
        doc.select("meta[property=og:image]").firstOrNull()?.attr("content")?.let { dto.image = it }
        doc.select("#icover_mid img.kniha_img.coverOnDetail").firstOrNull()?.attr("src")?.let { dto.image = it }

        // Authors
        val authors = mutableSetOf<String>()
        doc.select("span.author a, div.autori a, span.autor a").forEach { authors.add(it.text().trim()) }
        dto.authors = authors

        // Description (book summary)
        val descElement = doc.select("p.justify.new2.odtop, p.new2.odtop").firstOrNull()
        descElement?.text()?.trim()?.let {
            val cleaned = it.replace("... celý text", "").trim()
            if (cleaned.isNotEmpty()) dto.summary = cleaned
        }

        // Publisher
        doc.select("a[href*='/nakladatelstvi/']").firstOrNull()?.text()?.let { dto.publisher = it }

        // Year published
        doc.select("span.category:contains(Vydáno:)").firstOrNull()
            ?.nextElementSibling()?.text()?.trim()?.let { raw ->
                dto.publishedDate = raw.replace("[^0-9]".toRegex(), "")
            }

        // Page count (basic info on book page)
        doc.select("#more_book_info span.category:contains(Počet stran:)").firstOrNull()
            ?.nextElementSibling()?.text()?.trim()?.toIntOrNull()?.let { dto.pageCount = it }

        // Language
        doc.select("#more_book_info span.category:contains(Jazyk vydání:)").firstOrNull()
            ?.nextElementSibling()?.text()?.trim()?.let {
                dto.language = mapLanguage(it)
            }

        // ISBN
        doc.select("#more_book_info span.category:contains(ISBN:)").firstOrNull()
            ?.nextElementSibling()?.text()?.trim()?.let { raw ->
                val (isbn10, isbn13) = parseIsbn(raw)
                dto.isbn10 = isbn10
                dto.isbn13 = isbn13
            }

        // Tags (genres and tags)
        val tags = mutableSetOf<String>()
        doc.select("div.whiteBoxLight.odtop_big.oddown_big a.tag").forEach { tags.add(it.text().trim()) }
        doc.select("a.genre").forEach { tags.add(it.text().trim()) }
        dto.tags = tags

        // If no title found, parsing failed
        if (dto.title.isNullOrBlank()) return null
        return dto
    }

    /**
     * Parse ISBN string and detect ISBN10/ISBN13.
     */
    private fun parseIsbn(raw: String): Pair<String?, String?> {
        val cleaned = raw.replace("-", "").replace(" ", "")
        return when (cleaned.length) {
            7, 8, 10 -> Pair(cleaned, null)
            13 -> Pair(null, cleaned)
            else -> Pair(null, null)
        }
    }

    /** Map Czech language names to ISO code */
    private val languageMap = mapOf(
        "český" to "cs", "česká" to "cs",
        "slovenský" to "sk", "slovenská" to "sk",
        "německý" to "de",
        "polský" to "pl",
        "anglický" to "en",
        "francouzský" to "fr",
        "španělský" to "es",
        "italský" to "it",
    )

    private fun mapLanguage(dbLang: String): String? =
        languageMap[dbLang.lowercase()] ?: dbLang

    /**
     * Fetch and parse a document from URL using Jsoup.
     * Includes retries and user-agent header.
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
