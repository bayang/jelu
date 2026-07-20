package io.github.bayang.jelu.service.metadata

import io.github.bayang.jelu.dto.WikipediaPageResult
import io.github.bayang.jelu.dto.WikipediaSearchResult
import io.github.bayang.jelu.service.metadata.providers.USER_AGENT
import jakarta.annotation.Resource
import org.springframework.boot.info.BuildProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriBuilder

@Service
class WikipediaService(
    @Resource(name = "springRestClient") private val restClient: RestClient,
    private val buildProperties: BuildProperties,
) {
    // curl https://fr.wikipedia.org/w/rest.php/v1/search/title\?q\=stefan%20platteau\&limit\=5
    fun search(
        query: String,
        language: String = "en",
        limit: Int = 10,
    ): WikipediaSearchResult? {
        val mono: WikipediaSearchResult? =
            restClient
                .get()
                .uri { uriBuilder: UriBuilder ->
                    uriBuilder
                        .scheme("https")
                        .host("$language.wikipedia.org")
                        .path("/w/rest.php/v1/search/title")
                        .queryParam("q", query)
                        .queryParam("limit", limit)
                        .build()
                }.header(HttpHeaders.USER_AGENT, USER_AGENT + buildProperties.version)
                .exchange { clientRequest, clientResponse ->
                    if (clientResponse.statusCode == HttpStatus.OK) {
                        clientResponse.bodyTo(WikipediaSearchResult::class.java)
                    } else {
                        throw clientResponse.createException()
                    }
                }
        return mono
    }

    fun fetchPage(
        pageTitle: String,
        language: String = "en",
    ): WikipediaPageResult? {
        val mono: WikipediaPageResult? =
            restClient
                .get()
                .uri { uriBuilder: UriBuilder ->
                    uriBuilder
                        .scheme("https")
                        .host("$language.wikipedia.org")
                        .pathSegment("api", "rest_v1", "page", "summary", "{title}")
                        .queryParam("redirect", false)
                        .build(pageTitle)
                }.header(HttpHeaders.USER_AGENT, USER_AGENT + buildProperties.version)
                .exchange { clientRequest, clientResponse ->
                    if (clientResponse.statusCode == HttpStatus.OK) {
                        clientResponse.bodyTo(WikipediaPageResult::class.java)
                    } else {
                        throw clientResponse.createException()
                    }
                }
        return mono
    }
}
