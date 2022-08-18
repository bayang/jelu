package io.github.bayang.jelu.service.metadata.providers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.bayang.jelu.dto.MetadataDto
import io.github.bayang.jelu.service.quotes.KEY
import org.springframework.beans.factory.annotation.Value
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import java.time.LocalDate
import javax.annotation.Resource

class GoogleBooksIMetaDataProvider(
    @Resource(name = "restClient") private val restClient: WebClient,
    @Value("\${GOOGLE_API_KEY}") private val contentApiKey: String,
) : IMetaDataProvider {


    override fun fetchMetadata(isbn: String?, title: String?, authors: String?): Mono<MetadataDto> {
        return restClient.get()
            .uri { uriBuilder: UriBuilder ->
                uriBuilder
                    .scheme("https")
                    .host("www.googleapis.com")
                    .path("/books/v1/volumes")
                    .queryParam("q", "isbn:$isbn")
                    .queryParam("key", contentApiKey)
                    .build()
            }.exchangeToMono {
                if (it.statusCode() == HttpStatus.OK) {
                    it.bodyToMono(String::class.java).map { bodyString ->
                        parseBook(
                            ObjectMapper()
                                .readTree(bodyString)
                                .get("items")
                                .get(0)
                        )

                    }
                } else {
                    it.createException().flatMap { Mono.error { it } }
                }
            }
    }

    private fun parseBook(node: JsonNode): MetadataDto {
        val volumeInfo = node.get("volumeInfo")
        val identifiers = volumeInfo.get("industryIdentifiers").asIterable();
        return MetadataDto(
            title = volumeInfo.get("title").asText(),
            googleId = node.get("id").asText(),
            isbn10 = identifiers.find { it.get("type").asText() == "ISBN_10" }?.get("identifier")?.asText(),
            isbn13 = identifiers.find { it.get("type").asText() == "ISBN_13" }?.get("identifier")?.asText(),
            authors = volumeInfo.get("authors").asIterable().map { it.asText() }.toMutableSet(),
            image = volumeInfo.get("imageLinks").get("thumbnail").asText(),
            language = volumeInfo.get("language").asText(),
            publishedDate = volumeInfo.get("publishedDate").asText(),
            summary = node.get("searchInfo").get("textSnippet").asText()
        )
    }
}