package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dao.ReadingEventType
import io.github.bayang.jelu.dto.*
import io.github.bayang.jelu.service.BookService
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*
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

    @GetMapping(path = ["/userbooks/{id}"])
    fun userbookById(@PathVariable("id") userbookId: UUID) = repository.findUserBookById(userbookId)

    @GetMapping(path = ["/userbooks"])
    fun userbooks(principal: Authentication,
                  @RequestParam(name = "lastEventType", required = false) searchTerm: ReadingEventType?) {
        assertIsJeluUser(principal.principal)
        if (searchTerm != null) {
            repository.findUserBookByLastEvent((principal.principal as JeluUser).user.id.value, searchTerm)
        }
    }

    @GetMapping(path = ["/userbooks/me"])
    fun myBooks(principal: Authentication): List<UserBookLightDto> {
        assertIsJeluUser(principal.principal)
        return repository.findAllBooksByUser((principal.principal as JeluUser).user)
    }

    @GetMapping(path = ["/authors"])
    fun authors() = repository.findAllAuthors()

    @GetMapping(path = ["/authors/{id}"])
    fun authorById(@PathVariable("id") authorId: UUID) = repository.findAuthorsById(authorId)

    @PostMapping(path = ["/books"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun saveBook(@RequestBody @Valid book: BookCreateDto): BookDto {
        return repository.save(book, null)
    }

    @PostMapping(path = ["/books"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun saveBook(@RequestPart("book") @Valid book: BookCreateDto,
                 @RequestPart("file", required = false) file: MultipartFile?): BookDto {
        return repository.save(book, file)
    }

    @PostMapping(path = ["/userbooks"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun saveUserBook(@RequestBody @Valid book: CreateUserBookDto,
                     principal: Authentication): UserBookLightDto {
        return repository.save(book, (principal.principal as JeluUser).user, null)
    }

    @PostMapping(path = ["/userbooks"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun saveUserBook(@RequestPart("book") @Valid book: CreateUserBookDto, principal: Authentication,
                 @RequestPart("file", required = false) file: MultipartFile?): UserBookLightDto {
        return repository.save(book, (principal.principal as JeluUser).user, file)
    }

    @PostMapping(path = ["/authors"])
    fun saveAuthor(@RequestBody @Valid author: AuthorDto): AuthorDto {
        return repository.save(author)
    }

    @PutMapping(path = ["/books/{id}"])
    fun updateBook(@PathVariable("id") bookId: UUID, @RequestBody @Valid book: BookCreateDto): BookDto {
        return repository.update(bookId, book);
    }
    @PutMapping(path = ["/authors/{id}"])
    fun updateAuthor(@PathVariable("id") authorId: UUID, @RequestBody @Valid author: AuthorUpdateDto): AuthorDto {
        return repository.updateAuthor(authorId, author);
    }

    @PutMapping(path = ["/userbooks/{id}"])
    fun updateUserBook(@PathVariable("id") userBookId: UUID, @RequestBody @Valid book: UserBookUpdateDto): UserBookLightDto {
        return repository.update(userBookId, book);
    }
}

