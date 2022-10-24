package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dto.MetadataDto
import io.github.bayang.jelu.dto.WikipediaPageResult
import io.github.bayang.jelu.dto.WikipediaSearchResult
import io.github.bayang.jelu.errors.JeluException
import io.github.bayang.jelu.service.metadata.FetchMetadataService
import io.github.bayang.jelu.service.metadata.WikipediaService
import io.swagger.v3.oas.annotations.Operation
import mu.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1")
class MetadataController(
    private val properties: JeluProperties,
    private val metadataService: FetchMetadataService,
    private val wikipediaService: WikipediaService
) {

    @Operation(description = "fetch metadata from the configured providers")
    @GetMapping(path = ["/metadata"])
    fun fetchMetadata(
        @RequestParam(name = "isbn", required = false) isbn: String?,
        @RequestParam(name = "title", required = false) title: String?,
        @RequestParam(name = "authors", required = false) authors: String?
    ): Mono<MetadataDto> =
        if (properties.metadata.calibre.path.isNullOrBlank()) {
            throw JeluException("Automatic fetching of metadata is disabled, install calibre first")
        } else {
            metadataService.fetchMetadata(isbn, title, authors)
        }

    @Operation(description = "search the query in wikipedia")
    @GetMapping(path = ["/wikipedia/search"])
    fun searchWikipedia(
        @RequestParam(name = "query", required = true) query: String,
        @RequestParam(name = "language", defaultValue = "en") language: String,
    ): Mono<WikipediaSearchResult> {
        return wikipediaService.search(query, language)
    }

    @Operation(description = "retrieve page from wikipedia for given page title")
    @GetMapping(path = ["/wikipedia/page"])
    fun wikipediaPage(
        @RequestParam(name = "pageTitle", required = true) pageTitle: String,
        @RequestParam(name = "language", defaultValue = "en") language: String,
    ): Mono<WikipediaPageResult> {
        return wikipediaService.fetchPage(pageTitle, language)
    }
}
