package io.github.bayang.jelu.service.metadata.providers

import io.github.bayang.jelu.dto.MetadataDto
import io.github.bayang.jelu.dto.MetadataRequestDto
import reactor.core.publisher.Mono

interface IMetaDataProvider {
    fun fetchMetadata(
        metadataRequestDto: MetadataRequestDto,
        config: Map<String, String> = mapOf(),
    ): Mono<MetadataDto>?

    fun name(): String
}
