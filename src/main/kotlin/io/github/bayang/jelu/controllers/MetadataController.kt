package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dto.MetadataDto
import io.github.bayang.jelu.errors.JeluException
import io.github.bayang.jelu.service.metadata.FetchMetadataService
import mu.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1")
class MetadataController(
    private val properties: JeluProperties,
    private val metadataService: FetchMetadataService
) {

    @GetMapping(path = ["/metadata"])
    fun fetchMetadata(
        @RequestParam(name = "isbn", required = false) isbn: String?,
        @RequestParam(name = "title", required = false) title: String?,
        @RequestParam(name = "authors", required = false) authors: String?
    ): MetadataDto =
        if (properties.metadata.calibre.path.isNullOrBlank()) {
            throw JeluException("Automatic fetching of metadata is disabled, install calibre first")
        } else {
            metadataService.fetchMetadata(isbn, title, authors)
        }
}
