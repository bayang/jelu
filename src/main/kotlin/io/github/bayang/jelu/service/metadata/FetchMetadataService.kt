package io.github.bayang.jelu.service.metadata

import io.github.bayang.jelu.dto.MetadataDto
import io.github.bayang.jelu.dto.MetadataRequestDto
import io.github.bayang.jelu.service.metadata.providers.IMetaDataProvider
import io.github.bayang.jelu.utils.PluginInfoComparator
import io.github.oshai.kotlinlogging.KotlinLogging
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
        val metadata = MetadataDto()
        for ((idx, value) in pluginsToUse.withIndex()) {
            logger.trace { "fetching provider for plugin ${value.name} with order ${value.order} " }
            val provider = providers.find { value.name.equals(it.name(), true) }
            if (provider != null) {
                val res: Optional<MetadataDto>? = provider.fetchMetadata(metadataRequestDto, config)
                if (res != null && res.isPresent && res.get().filled) {
                    res.get().sourcePlugin = provider.name()
                    return res.get()
                } else if (res != null && res.isPresent && res.get().errors.isNotEmpty()) {
                    logger.error { "errors from plugin ${value.name} : ${res.get().errors}" }
                    metadata.errors.addAll(res.get().errors)
                } else {
                    logger.trace { "current plugin ${value.name} returned no result, trying next one. " }
                    continue
                }
            } else {
                logger.warn { "could not find provider for plugin info ${value.name}" }
            }
        }
        return metadata
    }
}
