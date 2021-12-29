package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.service.FetchMetadataService
import io.github.bayang.jelu.dto.MetadataDto
import mu.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api")
class MetadataController(
    private val properties: JeluProperties,
    private val metadataService: FetchMetadataService
) {

    @GetMapping(path = ["/metadata"])
    fun fetchMetadata(@RequestParam(name = "isbn", required = false) isbn: String?,
                      @RequestParam(name = "title", required = false) title: String?,
                      @RequestParam(name = "authors", required = false) authors: String?): MetadataDto
    = metadataService.fetchMetadata(isbn, title, authors)

}