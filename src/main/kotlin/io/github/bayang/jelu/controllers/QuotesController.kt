package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.dto.QuoteDto
import io.github.bayang.jelu.service.IQuoteProvider
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api")
class QuotesController(
    private val quotesProvider: IQuoteProvider
) {

    @GetMapping(path = ["/quotes"])
    fun quotes(@RequestParam(name = "query", required = false) query: String?): Mono<List<QuoteDto>> =
        quotesProvider.quotes(query)

    @GetMapping(path = ["/quotes/random"])
    fun quotes(): Mono<List<QuoteDto>> = quotesProvider.random()
}
