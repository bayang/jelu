package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dto.ServerSettingsDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class ServerSettingsController(
    private val properties: JeluProperties
) {

    @GetMapping(path = ["/server-settings"])
    fun getServerSettings(): ServerSettingsDto {
        return if (properties.metadata.calibre.path.isNullOrEmpty()) {
            ServerSettingsDto(metadataFetchEnabled = false, metadataFetchCalibreEnabled = false)
        } else {
            ServerSettingsDto(metadataFetchEnabled = true, metadataFetchCalibreEnabled = true)
        }
    }
}
