package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.dto.QuoteDto
import io.github.bayang.jelu.service.quotes.IQuoteProvider
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/v1")
class QuotesController(
    private val quotesProvider: IQuoteProvider,
) {
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "quotes list",
                content = [
                    (
                        Content(
                            mediaType = "application/json",
                            array = (
                                ArraySchema(
                                    schema =
                                        Schema(
                                            implementation = QuoteDto::class,
                                        ),
                                )
                            ),
                        )
                    ),
                ],
            ),
        ],
    )
    @GetMapping(path = ["/quotes"])
    fun quotes(
        @RequestParam(name = "query", required = false) query: String?,
    ): Mono<List<QuoteDto>> = quotesProvider.quotes(query)

    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "random quotes list",
                content = [
                    (
                        Content(
                            mediaType = "application/json",
                            array = (
                                ArraySchema(
                                    schema =
                                        Schema(
                                            implementation = QuoteDto::class,
                                        ),
                                )
                            ),
                        )
                    ),
                ],
            ),
        ],
    )
    @GetMapping(path = ["/quotes/random"])
    fun quotes(): Mono<List<QuoteDto>> = quotesProvider.random()
}
