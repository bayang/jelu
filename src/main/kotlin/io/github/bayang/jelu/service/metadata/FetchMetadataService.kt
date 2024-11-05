package io.github.bayang.jelu.service.metadata

import io.github.bayang.jelu.dto.MetadataDto
import io.github.bayang.jelu.dto.MetadataRequestDto
import io.github.bayang.jelu.service.metadata.providers.IMetaDataProvider
import io.github.bayang.jelu.utils.PluginInfoComparator
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.util.Optional

private val logger = KotlinLogging.logger {}

@Service
class FetchMetadataService(
    private val providers: List<IMetaDataProvider>,
    private val pluginInfoHolder: PluginInfoHolder,
) {

    fun fetchMetadata(
        metadataRequestDto: MetadataRequestDto,
        config: Map<String, String> = mapOf(),
    ): MetadataDto {
        var pluginsToUse = if (metadataRequestDto.plugins.isNullOrEmpty()) pluginInfoHolder.plugins() else metadataRequestDto.plugins
        pluginsToUse = pluginsToUse.toMutableList()
        // pluginInfoHolder sorts plugins, but plugins received via metadataRequestDto
        // might not be sorted
        pluginsToUse.sortWith(PluginInfoComparator)
        logger.trace { "plugins to use : $pluginsToUse" }
        for (plugin in pluginsToUse) {
            logger.trace { "fetching provider for plugin ${plugin.name} with order ${plugin.order} " }
            val provider = providers.find { plugin.name.equals(it.name(), true) }
            if (provider != null) {
                val res: Optional<MetadataDto> = provider.fetchMetadata(metadataRequestDto, config)
                if (res.isPresent) {
                    return res.get()
                }
            } else {
                logger.warn { "could not find provider for plugin info ${plugin.name}" }
            }
        }
        return MetadataDto()
    }
}
