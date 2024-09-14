package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.dao.Visibility
import io.github.bayang.jelu.dto.BookQuoteDto
import io.github.bayang.jelu.dto.CreateBookQuoteDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.dto.UpdateBookQuoteDto
import io.github.bayang.jelu.errors.JeluAuthenticationException
import io.github.bayang.jelu.service.BookQuoteService
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class BookQuoteController(
    private val bookQuoteService: BookQuoteService,
) {

    @PostMapping(path = ["/book-quotes"])
    fun createBookQuote(
        @RequestBody @Valid
        createBookQuoteDto: CreateBookQuoteDto,
        principal: Authentication,
    ): BookQuoteDto {
        return bookQuoteService.save(createBookQuoteDto, (principal.principal as JeluUser).user)
    }

    @GetMapping(path = ["/book-quotes"])
    fun bookQuotes(
        @RequestParam(name = "userId", required = false) userId: UUID?,
        @RequestParam(name = "bookId", required = false) bookId: UUID?,
        @RequestParam(name = "visibility", required = false) visibility: Visibility?,
        @PageableDefault(page = 0, size = 20, direction = Sort.Direction.DESC, sort = ["creationDate"]) @ParameterObject pageable: Pageable,
    ): Page<BookQuoteDto> {
        return bookQuoteService.find(userId, bookId, visibility, pageable)
    }

    @PutMapping(path = ["/book-quotes/{id}"])
    fun updateBookQuote(
        @PathVariable("id") bookQuoteId: UUID,
        @RequestBody @Valid
        updateBookQuoteDto: UpdateBookQuoteDto,
    ): BookQuoteDto {
        return bookQuoteService.update(bookQuoteId, updateBookQuoteDto)
    }

    @GetMapping(path = ["/book-quotes/{id}"])
    fun getBookQuote(
        @PathVariable("id") bookQuoteId: UUID,
        principal: Authentication?,
    ): BookQuoteDto {
        val quote = bookQuoteService.findById(bookQuoteId)
        if (quote.visibility == Visibility.PRIVATE && principal == null) {
            throw JeluAuthenticationException("Resource unauthorized")
        }
        return quote
    }

    @ApiResponse(responseCode = "204", description = "Deleted the book quote")
    @DeleteMapping(path = ["/book-quotes/{id}"])
    fun deleteBookQuoteById(@PathVariable("id") bookQuoteId: UUID): ResponseEntity<Unit> {
        bookQuoteService.delete(bookQuoteId)
        return ResponseEntity.noContent().build()
    }
}
