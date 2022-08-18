package io.github.bayang.jelu.service.metadata.providers

import io.github.bayang.jelu.dto.MetadataDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.reactive.function.client.WebClient
import javax.annotation.Resource

class GoogleBooksIMetaDataProvider(
    @Resource(name = "restClient") private val restClient: WebClient,
    @Value("\${GOOGLE_API_KEY}") private val contentApiKey: String,
) : IMetaDataProvider {


    override fun fetchMetadata(isbn: String?, title: String?, authors: String?): MetadataDto {
        TODO("Not yet implemented")
    }
}