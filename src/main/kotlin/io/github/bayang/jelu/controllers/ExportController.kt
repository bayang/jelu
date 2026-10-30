package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.service.exports.UserExportService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.File

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/exports")
class ExportController(
    val userExportService: UserExportService,
    private val properties: JeluProperties,
) {
    @ApiResponse(responseCode = "200", description = "The saved export csv file")
    @Operation(description = "Trigger a csv export file download")
    @GetMapping(path = ["/{exportFile}"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun exportCsv(
        principal: Authentication,
        @PathVariable exportFile: String,
    ): ResponseEntity<InputStreamResource> {
        val count = userExportService.existsByNameAndOwner((principal.principal as JeluUser).user, exportFile)
        if (count > 0) {
            val destFile = File(properties.files.imports, exportFile)
            val r = InputStreamResource(destFile.inputStream())
            return ResponseEntity
                .ok()
                .body(r)
        } else {
            return ResponseEntity.notFound().build()
        }
    }
}
