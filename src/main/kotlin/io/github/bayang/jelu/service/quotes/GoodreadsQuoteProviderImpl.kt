package io.github.bayang.jelu.service.quotes

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.bayang.jelu.dto.QuoteDto
import io.github.bayang.jelu.service.BookService
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriBuilder
import java.util.concurrent.TimeUnit
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

// eg https://www.goodreads.com/quotes/search?utf8=%E2%9C%93&q=pratchett&commit=Search
const val BASE_URL: String = "https://www.goodreads.com"

const val KEY: String = "quotes"

@Service
class GoodreadsQuoteProviderImpl(
    val bookService: BookService,
    @Resource(name = "springRestClient") private val restClient: RestClient,
) : IQuoteProvider {
    var cache: Cache<String, List<QuoteDto>> =
        Caffeine
            .newBuilder()
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .maximumSize(100)
            .build()

    override fun quotes(query: String?): List<QuoteDto> =
        if (!query.isNullOrBlank()) {
            fetch(query)
        } else {
            val res: List<QuoteDto>? = cache.getIfPresent(KEY)
            res ?: fetch(randomAuthor())
        }

    override fun random(): List<QuoteDto> {
        val mono: List<QuoteDto> =
            restClient
                .get()
                .uri { uriBuilder: UriBuilder ->
                    uriBuilder
                        .scheme("https")
                        .host("www.goodreads.com")
                        .path("/quotes")
                        .build()
                }.exchange { clientRequest, clientResponse ->
                    if (clientResponse.statusCode == HttpStatus.OK) {
                        parse(clientResponse.bodyTo(String::class.java))
                    } else {
                        throw clientResponse.createException()
                    }
                }
        return mono
    }

    private fun randomAuthor(): String {
        val page = bookService.findAllAuthors(null, pageable = Pageable.ofSize(20))
        return if (page.isEmpty) {
            ""
        } else {
            page.content[Random.nextInt(0, page.numberOfElements)].name
        }
    }

    fun fetch(query: String): List<QuoteDto> {
        val mono: List<QuoteDto> =
            restClient
                .get()
                .uri { uriBuilder: UriBuilder ->
                    uriBuilder
                        .scheme("https")
                        .host("www.goodreads.com")
                        .path("/quotes/search")
                        .queryParam("utf8", "✓")
                        .queryParam("commit", "Search")
                        .queryParam("q", query)
                        .build()
                }.exchange { clientRequest, clientResponse ->
                    if (clientResponse.statusCode == HttpStatus.OK) {
                        parse(clientResponse.bodyTo(String::class.java))
                    } else {
                        throw clientResponse.createException()
                    }
                }
        return mono
    }

    private fun parse(body: String?): List<QuoteDto> {
        logger.trace { "body : $body" }
        if (body.isNullOrBlank()) return mutableListOf()
        val doc = Jsoup.parse(body)
        val quotesElements: Elements = doc.select(".quoteText")
        val quotes = mutableListOf<QuoteDto>()
        for (elem in quotesElements) {
            quotes.add(quote(elem))
        }
        return quotes
    }

    fun quote(element: Element): QuoteDto {
        val url = element.select("a.authorOrTitle").attr("href")
        return QuoteDto(
            content = element.ownText(),
            author = element.select("span.authorOrTitle").text(),
            origin = element.select("a.authorOrTitle").text(),
            link = if (url.isBlank()) "" else BASE_URL + url,
        )
    }
}
