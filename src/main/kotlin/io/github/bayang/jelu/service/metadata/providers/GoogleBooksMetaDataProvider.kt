package io.github.bayang.jelu.service.metadata.providers

import io.github.bayang.jelu.dto.MetadataDto

class GoogleBooksMetaDataProvider : MetaDataProvider {
    override fun fetchMetadata(isbn: String?, title: String?, authors: String?): MetadataDto {
        TODO("Not yet implemented")
    }
}