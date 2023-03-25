package io.github.bayang.jelu.service.metadata.providers

import io.github.bayang.jelu.dto.MetadataDto
import io.github.bayang.jelu.dto.MetadataRequestDto
import io.github.bayang.jelu.service.metadata.PluginInfoHolder
import mu.KotlinLogging
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@Service
class DebugMetadataProvider:IMetaDataProvider {

    override fun fetchMetadata(
        metadataRequestDto: MetadataRequestDto,
        config: Map<String, String>
    ): Mono<MetadataDto>? {
        logger.debug { "debug plugin called with isbn ${metadataRequestDto.isbn}, title ${metadataRequestDto.title}, " +
            "authors ${metadataRequestDto.authors}, config $config, plugins ${metadataRequestDto.plugins}" }
        return null
    }

    override fun name(): String = PluginInfoHolder.jelu_debug
}
