package io.github.bayang.jelu.service.metadata

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dto.PluginInfo
import io.github.bayang.jelu.utils.PluginInfoComparator
import org.springframework.stereotype.Service

@Service
class PluginInfoHolder(
    private val properties: JeluProperties,
) {
    companion object {
        const val CALIBRE = "calibre"
        const val JELU_DEBUG = "jelu-debug"
    }

    private var pluginsList: List<PluginInfo> = listOf()
    private var pluginsComputed = false
    private var calibreComputed = false
    private var calibreEnabled = false

    fun plugins(): List<PluginInfo> {
        // return cached computation
        if (pluginsComputed) return pluginsList
        // compute plugins list and cache it
        val pluginInfoList: List<PluginInfo>? =
            properties.metadataProviders
                ?.filter { it.isEnabled }
                ?.map { PluginInfo(name = it.name, order = it.order) }
                ?.toList()
        val plugins = mutableListOf<PluginInfo>()
        if (calibreEnabled()) {
            plugins.add(PluginInfo(name = CALIBRE, order = properties.metadata.calibre.order))
        }
        if (!pluginInfoList.isNullOrEmpty()) {
            plugins.addAll(pluginInfoList)
        }
        plugins.sortWith(PluginInfoComparator)
        pluginsList = plugins
        pluginsComputed = true
        return pluginsList
    }

    fun calibreEnabled(): Boolean {
        if (calibreComputed) return calibreEnabled
        calibreEnabled =
            !properties.metadata.calibre.path
                .isNullOrEmpty()
        calibreComputed = true
        return calibreEnabled
    }
}
