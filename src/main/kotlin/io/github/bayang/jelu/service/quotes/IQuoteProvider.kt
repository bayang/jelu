package io.github.bayang.jelu.service.quotes

import io.github.bayang.jelu.dto.QuoteDto
import reactor.core.publisher.Mono

interface IQuoteProvider {

    fun quotes(query: String?): Mono<List<QuoteDto>>

    fun random(): Mono<List<QuoteDto>>
}
