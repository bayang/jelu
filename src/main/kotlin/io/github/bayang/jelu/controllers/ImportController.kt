package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dto.ImportConfigurationDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.service.import.CsvImportService
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
import javax.validation.Valid

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1")
class ImportController(
    val csvImportService: CsvImportService,
    private val properties: JeluProperties,
) {

    @PostMapping(path = ["/imports"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun importCsv(
        principal: Authentication,
        @RequestPart("importConfig") @Valid importConfig: ImportConfigurationDto,
        @RequestPart("file") file: MultipartFile
    ): ResponseEntity<Nothing> {
        val destFileName = FilenameUtils.getName(file.originalFilename)
        val destFile = File(properties.files.imports, destFileName)
        logger.debug { "target import file at ${destFile.absolutePath}" }
        file.transferTo(destFile)
        if (! destFile.exists()) {
            logger.error { "File ${destFile.absolutePath} not created, csv import aborted" }
        }
        csvImportService.import(destFile, (principal.principal as JeluUser).user.id.value, importConfig)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}
