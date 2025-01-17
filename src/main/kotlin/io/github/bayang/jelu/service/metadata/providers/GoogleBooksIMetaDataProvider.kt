package io.github.bayang.jelu.service.metadata.providers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dto.MetadataDto
import io.github.bayang.jelu.dto.MetadataRequestDto
import jakarta.annotation.Resource
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import java.time.Duration
import java.util.Optional

private val logger = KotlinLogging.logger {}

@Service
class GoogleBooksIMetaDataProvider(
    @Resource(name = "restClient") private val restClient: WebClient,
    private val properties: JeluProperties,
    private val objectMapper: ObjectMapper,
) : IMetaDataProvider {

    private val _name = "google"

    override fun fetchMetadata(
        metadataRequestDto: MetadataRequestDto,
        config: Map<String, String>,
    ): Optional<MetadataDto> {
        val googleProviderApiKey = getGoogleProviderApiKey()
        if (googleProviderApiKey.isNullOrBlank()) {
            logger.warn { "missing google books API key" }
            return Optional.empty()
        }
        val res = restClient.get()
            .uri { uriBuilder: UriBuilder ->
                uriBuilder
                    .scheme("https")
                    .host("www.googleapis.com")
                    .path("/books/v1/volumes")
                    .queryParam("q", query(metadataRequestDto))
                    .queryParam("key", googleProviderApiKey)
                    .build()
            }.exchangeToMono {
                if (it.statusCode() == HttpStatus.OK) {
                    it.bodyToMono(String::class.java).map { bodyString ->
                        val r = objectMapper.readTree(bodyString).get("items")
                        if (r == null) {
                            Optional.empty()
                        } else {
                            Optional.of(
                                parseBook(
                                    r.get(0),
                                ),
                            )
                        }
                    }
                } else {
                    logger.error { "error fetching metadata from google : ${it.statusCode()}" }
                    null
                }
            }
            .block(Duration.ofSeconds(60))
        if (res == null) {
            return Optional.empty()
        }
        return res
    }

    private fun query(metadataRequestDto: MetadataRequestDto): String {
        if (!metadataRequestDto.isbn.isNullOrBlank()) {
            return "isbn:${metadataRequestDto.isbn}"
        } else if (!metadataRequestDto.title.isNullOrBlank()) {
            var query = "intitle:${metadataRequestDto.title}"
            if (!metadataRequestDto.authors.isNullOrBlank()) {
                query += "+inauthor:${metadataRequestDto.authors}"
            }
            return query
        } else if (!metadataRequestDto.authors.isNullOrBlank()) {
            return "inauthor:${metadataRequestDto.authors}"
        }
        return ""
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
            image = extractImage(volumeInfo),
            language = volumeInfo.get("language").asText(),
            publishedDate = volumeInfo.get("publishedDate").asText(),
            summary = summary(node),
        )
    }

    private fun extractImage(node: JsonNode): String? {
        if (node.get("imageLinks") != null && node.get("imageLinks").get("thumbnail") != null) {
            return node.get("imageLinks").get("thumbnail").asText()
        }
        return null
    }

    private fun summary(node: JsonNode): String? {
        if (node.get("searchInfo") != null && node.get("searchInfo").get("textSnippet") != null) {
            return node.get("searchInfo").get("textSnippet").asText()
        }
        return null
    }
}
