package io.github.bayang.jelu.service.metadata.providers

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dto.MetadataDto
import io.github.bayang.jelu.dto.MetadataRequestDto
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriBuilder
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.util.Optional

private val logger = KotlinLogging.logger {}

@Service
class GoogleBooksIMetaDataProvider(
    @Resource(name = "springRestClient") private val restClient: RestClient,
    private val properties: JeluProperties,
    private val objectMapper: JsonMapper,
) : IMetaDataProvider {
    private val name = "google"
    private val scheme = "https"
    private val host = "www.googleapis.com"
    private val port = 443

    override fun fetchMetadata(
        metadataRequestDto: MetadataRequestDto,
        config: Map<String, String>,
    ): Optional<MetadataDto> {
        val googleProviderApiKey = getGoogleProviderApiKey()
        if (googleProviderApiKey.isNullOrBlank()) {
            logger.warn { "missing google books API key" }
            return Optional.empty()
        }
        val res =
            restClient
                .get()
                .uri { uriBuilder: UriBuilder ->
                    uriBuilder
                        .scheme(scheme)
                        .host(host)
                        .port(port)
                        .path("/books/v1/volumes")
                        .queryParam("q", query(metadataRequestDto))
                        .queryParam("key", googleProviderApiKey)
                        .build()
                }.exchange { request, response ->
                    if (response.statusCode == HttpStatus.OK) {
                        val b = response.bodyTo(String::class.java)
                        if (b.isNullOrBlank()) {
                            Optional.empty()
                        } else {
                            Optional.of(parseBook(objectMapper.readTree(b)["items"].get(0)))
                        }
                    } else {
                        null
                    }
                }
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

    override fun name(): String = name

    private fun getGoogleProviderApiKey(): String? =
        properties
            .metadataProviders
            ?.find { it.isEnabled && it.name == name }
            ?.apiKey

    private fun parseBook(node: JsonNode): MetadataDto {
        val volumeInfo = node.get("volumeInfo")
        val identifiers = volumeInfo.get("industryIdentifiers").asIterable()
        return MetadataDto(
            title = volumeInfo.get("title").asString(),
            googleId = node.get("id").asString(),
            isbn10 = identifiers.find { it.get("type").asString() == "ISBN_10" }?.get("identifier")?.asString(),
            isbn13 = identifiers.find { it.get("type").asString() == "ISBN_13" }?.get("identifier")?.asString(),
            authors = extractAuthors(volumeInfo),
            image = extractImage(volumeInfo),
            language = volumeInfo.get("language").asString(),
            publishedDate = volumeInfo.get("publishedDate").asString(),
            summary = summary(node),
        )
    }

    private fun extractAuthors(node: JsonNode): MutableSet<String> =
        if (node.get("authors") != null) {
            node
                .get("authors")
                .asIterable()
                .map { it.asString() }
                .toMutableSet()
        } else {
            mutableSetOf()
        }

    private fun extractImage(node: JsonNode): String? {
        if (node.get("imageLinks") != null && node.get("imageLinks").get("thumbnail") != null) {
            return node.get("imageLinks").get("thumbnail").asString()
        }
        return null
    }

    private fun summary(node: JsonNode): String? {
        if (node.get("searchInfo") != null && node.get("searchInfo").get("textSnippet") != null) {
            return node.get("searchInfo").get("textSnippet").asString()
        }
        return null
    }
}
