package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dto.MetadataDto
import io.github.bayang.jelu.dto.MetadataRequestDto
import io.github.bayang.jelu.dto.WikipediaPageResult
import io.github.bayang.jelu.dto.WikipediaSearchResult
import io.github.bayang.jelu.errors.JeluException
import io.github.bayang.jelu.service.metadata.FetchMetadataService
import io.github.bayang.jelu.service.metadata.FileMetadataService
import io.github.bayang.jelu.service.metadata.PluginInfoHolder
import io.github.bayang.jelu.service.metadata.WikipediaService
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import mu.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1")
class MetadataController(
    private val properties: JeluProperties,
    private val metadataService: FetchMetadataService,
    private val wikipediaService: WikipediaService,
    private val pluginInfoHolder: PluginInfoHolder,
    private val fileMetadataService: FileMetadataService,
) {

    @Operation(description = "fetch metadata from the configured providers")
    @GetMapping(path = ["/metadata"])
    fun fetchMetadata(
        @RequestParam(name = "isbn", required = false) isbn: String?,
        @RequestParam(name = "title", required = false) title: String?,
        @RequestParam(name = "authors", required = false) authors: String?,
    ): Mono<MetadataDto> =
        if (pluginInfoHolder.plugins().isEmpty()) {
            throw JeluException("Automatic fetching of metadata is disabled, install calibre or configure a metadata plugin")
        } else {
            metadataService.fetchMetadata(MetadataRequestDto(isbn, title, authors, listOf()))
        }

    @Operation(description = "fetch metadata from a file on the server")
    @GetMapping(path = ["/metadata/file"])
    fun fetchMetadataFromFile(
        @RequestParam(name = "filepath", required = true) path: String,
    ): Mono<MetadataDto> {
        val metadata = fileMetadataService.extractMetadata(path)
        return Mono.justOrEmpty(metadata)
    }

    @Operation(description = "fetch metadata from a file on the server")
    @PostMapping(path = ["/metadata/file"])
    fun fetchMetadataFromUploadedFile(
        @RequestPart("file", required = false) file: MultipartFile,
    ): Mono<MetadataDto> {
        val metadata = fileMetadataService.extractMetadata(file)
        return Mono.justOrEmpty(metadata)
    }

    @Operation(description = "fetch metadata from the configured providers")
    @PostMapping(path = ["/metadata"])
    fun fetchMetadata(
        @RequestBody @Valid
        metadataRequestDto: MetadataRequestDto,
    ): Mono<MetadataDto> {
        if (pluginInfoHolder.plugins().isEmpty()) {
            throw JeluException("Automatic fetching of metadata is disabled, install calibre or configure a metadata plugin")
        }
        return metadataService.fetchMetadata(metadataRequestDto)
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
