package io.github.bayang.jelu.service

import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder

@Service
class GoodreadsQuoteProviderImpl : IQuoteProvider {

    // eg https://www.goodreads.com/quotes/search?utf8=%E2%9C%93&q=pratchett&commit=Search
    val url: String = "https://www.goodreads.com/quotes/search?utf8=%E2%9C%93&commit=Search"

    override fun quotes(query: String): List<String> {
        var builder: UriComponentsBuilder = UriComponentsBuilder.fromUriString(url)
        builder.queryParam("q", query)
        builder.encode(Charsets.UTF_8)

        val uriString = builder.build().toUriString()

        return listOf(uriString)
    }
}