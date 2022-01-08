package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dao.ReadingEventType
import io.github.bayang.jelu.dto.*
import io.github.bayang.jelu.service.BookService
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
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
    private val properties: JeluProperties,
) {

    @GetMapping(path = ["/books"])
    fun books(@RequestParam(name = "title", required = false) title: String?,
              @RequestParam(name = "isbn10", required = false) isbn10: String?,
              @RequestParam(name = "isbn13", required = false) isbn13: String?,
              @RequestParam(name = "series", required = false) series: String?,
              @RequestParam(name = "page", required = false, defaultValue = "0") page: Long,
              @RequestParam(name = "pageSize", required = false, defaultValue = "20") pageSize: Long,
              principal: Authentication): Page<BookWithUserBookDto>
    = repository.findAll(title, isbn10, isbn13, series, page, pageSize, (principal.principal as JeluUser).user)

    @GetMapping(path = ["/books/{id}"])
    fun bookById(@PathVariable("id") bookId: UUID) = repository.findBookById(bookId)

    @GetMapping(path = ["/userbooks/{id}"])
    fun userbookById(@PathVariable("id") userbookId: UUID) = repository.findUserBookById(userbookId)

    @DeleteMapping(path = ["/userbooks/{id}"])
    fun deleteUserbookById(@PathVariable("id") userbookId: UUID): ResponseEntity<Unit> {
        repository.deleteUserBookById(userbookId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping(path = ["/books/{id}"])
    fun deletebookById(@PathVariable("id") bookId: UUID): ResponseEntity<Unit> {
        repository.deleteBookById(bookId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping(path = ["/books/{bookId}/tags/{tagId}"])
    fun deleteTagFromBook(@PathVariable("bookId") bookId: UUID,
                          @PathVariable("tagId") tagId: UUID): ResponseEntity<Unit> {
        repository.deleteTagFromBook(bookId, tagId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping(path = ["/tags/{tagId}"])
    fun deleteTagById(@PathVariable("tagId") tagId: UUID): ResponseEntity<Unit> {
        repository.deleteTagById(tagId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping(path = ["/books/{bookId}/authors/{authorId}"])
    fun deleteAuthorFromBook(@PathVariable("bookId") bookId: UUID,
                          @PathVariable("authorId") authorId: UUID): ResponseEntity<Unit> {
        repository.deleteAuthorFromBook(bookId, authorId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping(path = ["/authors/{authorId}"])
    fun deleteAuthorById(@PathVariable("authorId") authorId: UUID): ResponseEntity<Unit> {
        repository.deleteAuthorById(authorId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping(path = ["/userbooks"])
    fun userbooks(principal: Authentication,
                  @RequestParam(name = "lastEventType", required = false) searchTerm: ReadingEventType?,
                  @RequestParam(name = "toRead", required = false) toRead: Boolean?
    ): List<UserBookLightDto> {
        assertIsJeluUser(principal.principal)
        return repository.findUserBookByCriteria((principal.principal as JeluUser).user.id, searchTerm, toRead)
    }

    @GetMapping(path = ["/userbooks/me"])
    fun myBooks(principal: Authentication,
                @RequestParam(name = "page", required = false, defaultValue = "0") page: Long,
                @RequestParam(name = "pageSize", required = false, defaultValue = "20") pageSize: Long
    ): Page<UserBookLightDto> {
        assertIsJeluUser(principal.principal)
        return repository.findAllBooksByUser((principal.principal as JeluUser).user, page, pageSize)
    }

    @GetMapping(path = ["/authors"])
    fun authors(@RequestParam(name = "name", required = false) name: String?,
                @RequestParam(name = "page", required = false, defaultValue = "0") page: Long,
                @RequestParam(name = "pageSize", required = false, defaultValue = "20") pageSize: Long): Page<AuthorDto> {
        return repository.findAllAuthors(name, page, pageSize)
    }

    @GetMapping(path = ["/tags"])
    fun tags(@RequestParam(name = "name", required = false) name: String?): List<TagDto> {
        return if (name.isNullOrBlank()) {
            repository.findAllTags()
        } else {
            repository.findTagsByName(name)
        }
    }

    @GetMapping(path = ["/tags/{id}"])
    fun tagById(@PathVariable("id") tagId: UUID,
                principal: Authentication): TagDto {
        assertIsJeluUser(principal.principal)
        return repository.findTagById(tagId, (principal.principal as JeluUser).user)
    }

    @GetMapping(path = ["/tags/{id}/books"])
    fun tagBooksById(@PathVariable("id") tagId: UUID,
                @RequestParam(name = "page", required = false, defaultValue = "0") page: Long,
                @RequestParam(name = "pageSize", required = false, defaultValue = "20") pageSize: Long,
                principal: Authentication): Page<BookWithUserBookDto> {
        assertIsJeluUser(principal.principal)
        return repository.findTagBooksById(tagId, (principal.principal as JeluUser).user, page, pageSize)
    }

    @GetMapping(path = ["/authors/{id}"])
    fun authorById(@PathVariable("id") authorId: UUID) = repository.findAuthorsById(authorId)

    @GetMapping(path = ["/authors/{id}/books"])
    fun authorBooksById(@PathVariable("id") authorId: UUID,
                        @RequestParam(name = "page", required = false, defaultValue = "0") page: Long,
                        @RequestParam(name = "pageSize", required = false, defaultValue = "20") pageSize: Long,
                        principal: Authentication): Page<BookWithUserBookDto>
    = repository.findAuthorBooksById(authorId, (principal.principal as JeluUser).user, page, pageSize)

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
    fun updateBook(@PathVariable("id") bookId: UUID, @RequestBody @Valid book: BookUpdateDto): BookDto {
        return repository.update(bookId, book);
    }
    @PutMapping(path = ["/authors/{id}"])
    fun updateAuthor(@PathVariable("id") authorId: UUID, @RequestBody @Valid author: AuthorUpdateDto): AuthorDto {
        return repository.updateAuthor(authorId, author);
    }

    @PutMapping(path = ["/userbooks/{id}"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun updateUserBook(@PathVariable("id") userBookId: UUID, @RequestBody @Valid book: UserBookUpdateDto): UserBookLightDto {
        return repository.update(userBookId, book);
    }

    @PutMapping(path = ["/userbooks/{id}"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateUserBook(@PathVariable("id") userBookId: UUID, @RequestPart("book")  @Valid book: UserBookUpdateDto,
                       @RequestPart("file", required = false) file: MultipartFile?): UserBookLightDto {
        return repository.update(userBookId, book, file);
    }
}

