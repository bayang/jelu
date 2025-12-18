package io.github.bayang.jelu.service.metadata.providers

import io.github.bayang.jelu.dto.MetadataDto
import io.github.bayang.jelu.dto.MetadataRequestDto
import io.github.bayang.jelu.service.metadata.PluginInfoHolder
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.util.Optional

private val logger = KotlinLogging.logger {}

@Service
class DebugMetadataProvider : IMetaDataProvider {

    override fun fetchMetadata(
        metadataRequestDto: MetadataRequestDto,
        config: Map<String, String>,
    ): Optional<MetadataDto> {
        logger.debug {
            "debug plugin called with isbn ${metadataRequestDto.isbn}, title ${metadataRequestDto.title}, " +
                "authors ${metadataRequestDto.authors}, config $config, plugins ${metadataRequestDto.plugins}"
        }
        return Optional.empty()
    }

    override fun name(): String = PluginInfoHolder.jelu_debug
}
