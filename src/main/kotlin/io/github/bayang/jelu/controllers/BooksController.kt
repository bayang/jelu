package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dto.AuthorDto
import io.github.bayang.jelu.dto.BookDto
import io.github.bayang.jelu.dto.BookUpdateDto
import io.github.bayang.jelu.service.BookService
import mu.KotlinLogging
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.validation.Valid

private val logger = KotlinLogging.logger {}

@RestController
class BooksController(
    private val repository: BookService,
    private val properties: JeluProperties
    ) {

    @GetMapping(path = ["/books"])
    fun books(@RequestParam(name = "q", required = false) searchTerm: String?) = repository.findAll(searchTerm)

    @GetMapping(path = ["/books/{id}"])
    fun bookById(@PathVariable("id") bookId: UUID) = repository.findBookById(bookId)

    @GetMapping(path = ["/authors"])
    fun authors() = repository.findAllAuthors()

    @GetMapping(path = ["/authors/{id}"])
    fun authorById(@PathVariable("id") authorId: UUID) = repository.findAuthorsById(authorId)

    @PostMapping(path = ["/books"])
    fun saveBook(@RequestBody @Valid book: BookDto): BookDto {
        return repository.save(book)
    }
    @PostMapping(path = ["/authors"])
    fun saveAuthor(@RequestBody @Valid author: AuthorDto): AuthorDto {
        return repository.save(author)
    }
    @PutMapping(path = ["/books/{id}"])
    fun updateBook(@PathVariable("id") bookId: UUID, @RequestBody @Valid book: BookUpdateDto): BookDto {
        return repository.update(bookId, book);
    }
}