package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dto.ImportConfigurationDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.service.exports.CsvExportService
import io.github.bayang.jelu.service.imports.CsvImportService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import mu.KotlinLogging
import org.apache.commons.io.FilenameUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.util.Locale

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1")
class ImportController(
    val csvImportService: CsvImportService,
    val csvExportService: CsvExportService,
    private val properties: JeluProperties,
) {

    @ApiResponse(responseCode = "201", description = "Imported the csv file")
    @Operation(description = "Trigger a csv import")
    @PostMapping(path = ["/imports"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun importCsv(
        principal: Authentication,
        @RequestPart("importConfig") @Valid importConfig: ImportConfigurationDto,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<Nothing> {
        val destFileName = FilenameUtils.getName(file.originalFilename)
        val destFile = File(properties.files.imports, destFileName)
        logger.debug { "target import file at ${destFile.absolutePath}" }
        file.transferTo(destFile)
        if (!destFile.exists()) {
            logger.error { "File ${destFile.absolutePath} not created, csv import aborted" }
        }
        csvImportService.import(destFile, (principal.principal as JeluUser).user.id!!, importConfig)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @ApiResponse(responseCode = "201", description = "Saved the export csv request")
    @Operation(description = "Trigger a csv export")
    @PostMapping(path = ["/exports"])
    fun exportCsv(
        principal: Authentication,
        locale: Locale,
    ): ResponseEntity<Nothing> {
        csvExportService.export((principal.principal as JeluUser).user, locale)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}
