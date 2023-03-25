package io.github.bayang.jelu.dto

data class ServerSettingsDto(
    /**
     * Is there any metadata provider activated at all
     */
    val metadataFetchEnabled: Boolean,
    /**
     * Is the calibre metadata provider activated
     */
    val metadataFetchCalibreEnabled: Boolean,
    val appVersion: String,
    val ldapEnabled: Boolean,
    val metadataPlugins: List<PluginInfo> = listOf()
)
