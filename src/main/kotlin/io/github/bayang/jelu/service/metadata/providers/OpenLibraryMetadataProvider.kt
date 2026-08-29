package io.github.bayang.jelu.service.metadata.providers

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
class OpenLibraryMetadataProvider(
    @Resource(name = "springRestClient") private val restClient: RestClient,
    private val objectMapper: JsonMapper,
) : IMetaDataProvider {
    private val name = "openlibrary"

    override fun fetchMetadata(
        metadataRequestDto: MetadataRequestDto,
        config: Map<String, String>,
    ): Optional<MetadataDto> {
        if (metadataRequestDto.isbn.isNullOrBlank()) {
            return Optional.empty()
        }
        val bibKey = "ISBN:${metadataRequestDto.isbn}"
        val res =
            restClient
                .get()
                .uri { uriBuilder: UriBuilder ->
                    uriBuilder
                        .scheme("https")
                        .host("openlibrary.org")
                        .path("/api/books")
                        .queryParam("bibkeys", bibKey)
                        .queryParam("format", "json")
                        .queryParam("jscmd", "data")
                        .build()
                }.exchange { request, response ->
                    if (response.statusCode == HttpStatus.OK) {
                        val b = response.bodyTo(String::class.java)
                        if (b.isNullOrBlank()) {
                            Optional.empty()
                        } else {
                            val root = objectMapper.readTree(b)
                            val book = root.get(bibKey)
                            if (book == null) {
                                Optional.empty()
                            } else {
                                Optional.of(parseBook(book, metadataRequestDto.isbn))
                            }
                        }
                    } else {
                        logger.error { "error fetching metadata from openlibrary : ${response.statusCode}" }
                        Optional.empty()
                    }
                }
        return res
    }

    override fun name(): String = name

    private fun parseBook(
        node: JsonNode,
        requestedIsbn: String?,
    ): MetadataDto =
        MetadataDto(
            title = node.get("title")?.asText(),
            isbn10 = extractIdentifier(node, "isbn_10") ?: requestedIsbn,
            isbn13 = extractIdentifier(node, "isbn_13"),
            authors = extractAuthors(node),
            image = extractImage(node),
            publisher = extractPublisher(node),
            publishedDate = node.get("publish_date")?.asText(),
            openlibraryId = node.get("key")?.asText()?.removePrefix("/books/"),
        )

    private fun extractIdentifier(
        node: JsonNode,
        key: String,
    ): String? {
        val identifiers = node.get("identifiers") ?: return null
        val arr = identifiers.get(key) ?: return null
        return if (arr.isArray && arr.size() > 0) arr.get(0).asText() else null
    }

    private fun extractAuthors(node: JsonNode): MutableSet<String> =
        if (node.get("authors") != null) {
            node
                .get("authors")
                .asIterable()
                .mapNotNull { it.get("name")?.asText() }
                .toMutableSet()
        } else {
            mutableSetOf()
        }

    private fun extractImage(node: JsonNode): String? {
        val cover = node.get("cover") ?: return null
        return cover.get("large")?.asText() ?: cover.get("medium")?.asText()
    }

    private fun extractPublisher(node: JsonNode): String? {
        val publishers = node.get("publishers") ?: return null
        return if (publishers.isArray && publishers.size() > 0) publishers.get(0).get("name")?.asText() else null
    }
}
