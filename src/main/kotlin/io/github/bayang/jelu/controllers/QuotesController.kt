package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.service.IQuoteProvider
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class QuotesController(
    private val quotesProvider: IQuoteProvider
) {

    @GetMapping(path = ["/quotes"])
    fun quotes(@RequestParam(name = "query", required = true) query: String): List<String> {
        return quotesProvider.quotes(query)
    }


}