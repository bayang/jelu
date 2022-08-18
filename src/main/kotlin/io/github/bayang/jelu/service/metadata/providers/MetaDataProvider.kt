package io.github.bayang.jelu.service.metadata.providers

import io.github.bayang.jelu.dto.MetadataDto

interface MetaDataProvider {
    fun fetchMetadata(
        isbn: String?,
        title: String?,
        authors: String?
    ): MetadataDto
}