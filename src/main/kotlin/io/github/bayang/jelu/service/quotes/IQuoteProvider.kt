package io.github.bayang.jelu.service.quotes

import io.github.bayang.jelu.dto.QuoteDto

interface IQuoteProvider {
    fun quotes(query: String?): List<QuoteDto>

    fun random(): List<QuoteDto>
}
