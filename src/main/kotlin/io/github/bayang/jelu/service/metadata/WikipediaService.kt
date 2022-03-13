package io.github.bayang.jelu.service.metadata

import io.github.bayang.jelu.dto.WikipediaPageResult
import io.github.bayang.jelu.dto.WikipediaSearchResult
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import javax.annotation.Resource

@Service
class WikipediaService(
    @Resource(name = "restClient") val restClient: WebClient
) {

    // curl https://fr.wikipedia.org/w/rest.php/v1/search/title\?q\=stefan%20platteau\&limit\=5
    fun search(query: String, language: String = "en", limit: Int = 10): Mono<WikipediaSearchResult> {
        val mono: Mono<WikipediaSearchResult> = restClient.get()
            .uri { uriBuilder: UriBuilder ->
                uriBuilder
                    .scheme("https")
                    .host("$language.wikipedia.org")
                    .path("/w/rest.php/v1/search/title")
                    .queryParam("q", query)
                    .queryParam("limit", limit)
                    .build()
            }
            .exchangeToMono {
                if (it.statusCode() == HttpStatus.OK) {
                    it.bodyToMono(WikipediaSearchResult::class.java)
                } else {
                    it.createException().flatMap { Mono.error { it } }
                }
            }
        return mono
    }

    fun fetchPage(pageTitle: String, language: String = "en"): Mono<WikipediaPageResult> {
        val mono: Mono<WikipediaPageResult> = restClient.get()
            .uri { uriBuilder: UriBuilder ->
                uriBuilder
                    .scheme("https")
                    .host("$language.wikipedia.org")
                    .pathSegment("api", "rest_v1", "page", "summary", "{title}")
                    .queryParam("redirect", false)
                    .build(pageTitle)
            }
            .exchangeToMono {
                if (it.statusCode() == HttpStatus.OK) {
                    it.bodyToMono(WikipediaPageResult::class.java)
                } else {
                    it.createException().flatMap { Mono.error { it } }
                }
            }
        return mono
    }
}
