package io.github.bayang.jelu.service.metadata.providers

import io.github.bayang.jelu.dto.MetadataDto
import io.github.bayang.jelu.dto.MetadataRequestDto
import java.util.Optional

interface IMetaDataProvider {
    fun fetchMetadata(
        metadataRequestDto: MetadataRequestDto,
        config: Map<String, String> = mapOf(),
    ): Optional<MetadataDto>?

    fun name(): String
}
