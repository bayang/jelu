package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dto.ServerSettingsDto
import io.swagger.v3.oas.annotations.Operation
import org.springframework.boot.info.BuildProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class ServerSettingsController(
    private val properties: JeluProperties,
    private val buildProperties: BuildProperties
) {

    @Operation(description = "Get the capabilities configured for this server, eg : is the metadata binary installed etc...")
    @GetMapping(path = ["/server-settings"])
    fun getServerSettings(): ServerSettingsDto {
        return if (properties.metadata.calibre.path.isNullOrEmpty()) {
            ServerSettingsDto(
                metadataFetchEnabled = false,
                metadataFetchCalibreEnabled = false,
                buildProperties.version,
                ldapEnabled = properties.auth.ldap.enabled
            )
        } else {
            ServerSettingsDto(
                metadataFetchEnabled = true,
                metadataFetchCalibreEnabled = true,
                buildProperties.version,
                ldapEnabled = properties.auth.ldap.enabled
            )
        }
    }
}
