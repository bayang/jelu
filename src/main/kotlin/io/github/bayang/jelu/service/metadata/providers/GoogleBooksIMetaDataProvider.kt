package io.github.bayang.jelu.service.metadata.providers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dto.MetadataDto
import io.github.bayang.jelu.dto.MetadataRequestDto
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import javax.annotation.Resource

private val logger = KotlinLogging.logger {}

@Service
class GoogleBooksIMetaDataProvider(
    @Resource(name = "restClient") private val restClient: WebClient,
    private val properties: JeluProperties
) : IMetaDataProvider {

    private val _name = "google"

    override fun fetchMetadata(
        metadataRequestDto: MetadataRequestDto,
        config: Map<String, String>
    ): Mono<MetadataDto>? {
        val googleProviderApiKey = getGoogleProviderApiKey()
        if (googleProviderApiKey.isNullOrBlank() || metadataRequestDto.isbn.isNullOrBlank()) {
            return null
        }
        return restClient.get()
            .uri { uriBuilder: UriBuilder ->
                uriBuilder
                    .scheme("https")
                    .host("www.googleapis.com")
                    .path("/books/v1/volumes")
                    .queryParam("q", "isbn:${metadataRequestDto.isbn}")
                    .queryParam("key", googleProviderApiKey)
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
                    logger.error { "error fetching metadata from google : ${it.statusCode()}" }
                    null
                }
            }
    }

    override fun name(): String {
        return _name
    }

    private fun getGoogleProviderApiKey(): String? = properties
        .metadataProviders
        ?.find { it.isEnabled && it.name == _name }
        ?.apiKey

    private fun parseBook(node: JsonNode): MetadataDto {
        val volumeInfo = node.get("volumeInfo")
        val identifiers = volumeInfo.get("industryIdentifiers").asIterable()
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
