package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dao.Author
import io.github.bayang.jelu.dto.*
import io.github.bayang.jelu.errors.JeluException
import io.github.bayang.jelu.service.BookService
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*
import javax.servlet.annotation.MultipartConfig
import javax.validation.Valid

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api")
class BooksController(
    private val repository: BookService,
    private val properties: JeluProperties
    ) {

    @GetMapping(path = ["/books"])
    fun books(@RequestParam(name = "q", required = false) searchTerm: String?) = repository.findAll(searchTerm)

    @GetMapping(path = ["/books/{id}"])
    fun bookById(@PathVariable("id") bookId: UUID) = repository.findBookById(bookId)

    @GetMapping(path = ["/books/me"])
    fun myBooks(principal: Authentication): List<UserBookLightDto> {
        if (principal.principal !is JeluUser) {
            throw JeluException("Logged in user/provided credentials cannot get his ReadingEvents")
        }
        return repository.findAllBooksByUser((principal.principal as JeluUser).user)
    }

    @GetMapping(path = ["/authors"])
    fun authors() = repository.findAllAuthors()

    @GetMapping(path = ["/authors/{id}"])
    fun authorById(@PathVariable("id") authorId: UUID) = repository.findAuthorsById(authorId)

    @PostMapping(path = ["/books"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun saveBook(@RequestBody @Valid book: CreateBookDto, principal: Authentication): BookDto {
        return repository.save(book, (principal.principal as JeluUser).user)
    }

    @PostMapping(path = ["/books"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun saveBook(@RequestPart("book") @Valid book: CreateBookDto, principal: Authentication,
                 @RequestPart("file", required = false) file: MultipartFile?): BookDto {
        return repository.save(book, (principal.principal as JeluUser).user, file)
    }

    @PostMapping(path = ["/authors"])
    fun saveAuthor(@RequestBody @Valid author: AuthorDto): AuthorDto {
        return repository.save(author)
    }
    @PutMapping(path = ["/books/{id}"])
    fun updateBook(@PathVariable("id") bookId: UUID, @RequestBody @Valid book: BookUpdateDto): BookDto {
        return repository.update(bookId, book);
    }

    @PutMapping(path = ["/authors/{id}"])
    fun updateAuthor(@PathVariable("id") authorId: UUID, @RequestBody @Valid author: AuthorUpdateDto): AuthorDto {
        return repository.updateAuthor(authorId, author);
    }
}