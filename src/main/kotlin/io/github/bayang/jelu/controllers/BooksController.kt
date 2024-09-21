package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dao.ReadingEventType
import io.github.bayang.jelu.dto.AuthorDto
import io.github.bayang.jelu.dto.AuthorUpdateDto
import io.github.bayang.jelu.dto.BookCreateDto
import io.github.bayang.jelu.dto.BookDto
import io.github.bayang.jelu.dto.BookUpdateDto
import io.github.bayang.jelu.dto.CreateUserBookDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.dto.LibraryFilter
import io.github.bayang.jelu.dto.Role
import io.github.bayang.jelu.dto.SeriesDto
import io.github.bayang.jelu.dto.SeriesUpdateDto
import io.github.bayang.jelu.dto.TagDto
import io.github.bayang.jelu.dto.UserBookBulkUpdateDto
import io.github.bayang.jelu.dto.UserBookLightDto
import io.github.bayang.jelu.dto.UserBookUpdateDto
import io.github.bayang.jelu.dto.UserBookWithoutEventsAndUserDto
import io.github.bayang.jelu.dto.assertIsJeluUser
import io.github.bayang.jelu.service.BookService
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import mu.KotlinLogging
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.MediaType
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
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1")
class BooksController(
    private val repository: BookService,
    private val properties: JeluProperties,
) {

    @GetMapping(path = ["/books"])
    fun books(
        @RequestParam(name = "q", required = false) query: String?,
        @RequestParam(name = "libraryFilter", required = false) libraryFilter: LibraryFilter?,
        @PageableDefault(page = 0, size = 20, direction = Sort.Direction.ASC, sort = ["title"]) @ParameterObject pageable: Pageable,
        principal: Authentication,
    ): Page<BookDto> {
        return repository.findAll(
            query,
            pageable,
            (principal.principal as JeluUser).user,
            libraryFilter ?: LibraryFilter.ANY,
        )
    }

    @GetMapping(path = ["/books/{id}"])
    fun bookById(@PathVariable("id") bookId: UUID) = repository.findBookById(bookId)

    @GetMapping(path = ["/userbooks/{id}"])
    fun userbookById(@PathVariable("id") userbookId: UUID) = repository.findUserBookById(userbookId)

    @ApiResponse(responseCode = "204", description = "Deleted the userbook")
    @DeleteMapping(path = ["/userbooks/{id}"])
    fun deleteUserbookById(@PathVariable("id") userbookId: UUID): ResponseEntity<Unit> {
        repository.deleteUserBookById(userbookId)
        return ResponseEntity.noContent().build()
    }

    @ApiResponse(responseCode = "204", description = "Deleted the book")
    @DeleteMapping(path = ["/books/{id}"])
    fun deletebookById(@PathVariable("id") bookId: UUID): ResponseEntity<Unit> {
        repository.deleteBookById(bookId)
        return ResponseEntity.noContent().build()
    }

    @ApiResponse(responseCode = "204", description = "Deleted the tag from the book")
    @DeleteMapping(path = ["/books/{bookId}/tags/{tagId}"])
    fun deleteTagFromBook(
        @PathVariable("bookId") bookId: UUID,
        @PathVariable("tagId") tagId: UUID,
    ): ResponseEntity<Unit> {
        repository.deleteTagFromBook(bookId, tagId)
        return ResponseEntity.noContent().build()
    }

    @ApiResponse(responseCode = "204", description = "Deleted the tag")
    @DeleteMapping(path = ["/tags/{tagId}"])
    fun deleteTagById(@PathVariable("tagId") tagId: UUID): ResponseEntity<Unit> {
        repository.deleteTagById(tagId)
        return ResponseEntity.noContent().build()
    }

    @ApiResponse(responseCode = "204", description = "Deleted the author from the book")
    @DeleteMapping(path = ["/books/{bookId}/authors/{authorId}"])
    fun deleteAuthorFromBook(
        @PathVariable("bookId") bookId: UUID,
        @PathVariable("authorId") authorId: UUID,
    ): ResponseEntity<Unit> {
        repository.deleteAuthorFromBook(bookId, authorId)
        return ResponseEntity.noContent().build()
    }

    @ApiResponse(responseCode = "204", description = "Deleted the translator from the book")
    @DeleteMapping(path = ["/books/{bookId}/translators/{translatorId}"])
    fun deleteTranslatorFromBook(
        @PathVariable("bookId") bookId: UUID,
        @PathVariable("translatorId") translatorId: UUID,
    ): ResponseEntity<Unit> {
        repository.deleteTranslatorFromBook(bookId, translatorId)
        return ResponseEntity.noContent().build()
    }

    @ApiResponse(responseCode = "204", description = "Deleted the author")
    @DeleteMapping(path = ["/authors/{authorId}"])
    fun deleteAuthorById(@PathVariable("authorId") authorId: UUID): ResponseEntity<Unit> {
        repository.deleteAuthorById(authorId)
        return ResponseEntity.noContent().build()
    }

    @ApiResponse(responseCode = "204", description = "Deleted the series from the book")
    @DeleteMapping(path = ["/books/{bookId}/series/{seriesId}"])
    fun deleteSeriesFromBook(
        @PathVariable("bookId") bookId: UUID,
        @PathVariable("seriesId") seriesId: UUID,
    ): ResponseEntity<Unit> {
        repository.deleteSeriesFromBook(bookId, seriesId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping(path = ["/userbooks"])
    fun userbooks(
        principal: Authentication,
        @RequestParam(name = "lastEventTypes", required = false) eventTypes: List<ReadingEventType>?,
        @RequestParam(name = "bookId", required = false) bookId: UUID?,
        @RequestParam(name = "toRead", required = false) toRead: Boolean?,
        @RequestParam(name = "owned", required = false) owned: Boolean?,
        @RequestParam(name = "borrowed", required = false) borrowed: Boolean?,
        @RequestParam(name = "userId", required = false) userId: UUID?,
        @PageableDefault(page = 0, size = 20, direction = Sort.Direction.DESC, sort = ["modificationDate"]) @ParameterObject pageable: Pageable,
    ): Page<UserBookWithoutEventsAndUserDto> {
        assertIsJeluUser(principal.principal)
        val finalUserId = userId ?: (principal.principal as JeluUser).user.id.value
        return repository.findUserBookByCriteria(finalUserId, bookId, eventTypes, toRead, owned, borrowed, pageable)
    }

    @GetMapping(path = ["/authors"])
    fun authors(
        @RequestParam(name = "name", required = false) name: String?,
        @PageableDefault(page = 0, size = 20, direction = Sort.Direction.ASC, sort = ["name"]) @ParameterObject pageable: Pageable,
    ): Page<AuthorDto> {
        return repository.findAllAuthors(name, pageable)
    }

    @GetMapping(path = ["/tags"])
    fun tags(
        @RequestParam(name = "name", required = false) name: String?,
        @PageableDefault(page = 0, size = 20, direction = Sort.Direction.ASC, sort = ["name"]) @ParameterObject pageable: Pageable,
    ): Page<TagDto> = repository.findAllTags(name, pageable)

    @GetMapping(path = ["/tags/{id}"])
    fun tagById(
        @PathVariable("id") tagId: UUID,
        principal: Authentication,
    ): TagDto {
        assertIsJeluUser(principal.principal)
        return repository.findTagById(tagId, (principal.principal as JeluUser).user)
    }

    @GetMapping(path = ["/tags/{id}/books"])
    fun tagBooksById(
        @PathVariable("id") tagId: UUID,
        @RequestParam(name = "libraryFilter", required = false) libraryFilter: LibraryFilter?,
        @RequestParam(name = "lastEventTypes", required = false) eventTypes: List<ReadingEventType>?,
        @PageableDefault(page = 0, size = 20, direction = Sort.Direction.ASC, sort = ["title"]) @ParameterObject pageable: Pageable,
        principal: Authentication,
    ): Page<BookDto> {
        assertIsJeluUser(principal.principal)
        return repository.findTagBooksById(tagId, (principal.principal as JeluUser).user, pageable, libraryFilter ?: LibraryFilter.ANY, eventTypes)
    }

    @GetMapping(path = ["/tags/orphans"])
    fun orphanTags(
        @PageableDefault(page = 0, size = 20, direction = Sort.Direction.ASC, sort = ["name"]) @ParameterObject pageable: Pageable,
    ): Page<TagDto> = repository.findOrphanTags(pageable)

    @GetMapping(path = ["/series"])
    fun series(
        @RequestParam(name = "name", required = false) name: String?,
        @PageableDefault(page = 0, size = 20, direction = Sort.Direction.ASC, sort = ["name"]) @ParameterObject pageable: Pageable,
        principal: Authentication,
    ): Page<SeriesDto> {
        assertIsJeluUser(principal.principal)
        return repository.findAllSeries(name, (principal.principal as JeluUser).user.id.value, pageable)
    }

    @GetMapping(path = ["/series/{id}"])
    fun seriesById(
        @PathVariable("id") seriesId: UUID,
        principal: Authentication,
    ): SeriesDto {
        assertIsJeluUser(principal.principal)
        return repository.findSeriesById(seriesId, (principal.principal as JeluUser).user.id.value)
    }

    @GetMapping(path = ["/series/{id}/books"])
    fun seriesBooksById(
        @PathVariable("id") seriesId: UUID,
        @RequestParam(name = "libraryFilter", required = false) libraryFilter: LibraryFilter?,
        @PageableDefault(page = 0, size = 20, direction = Sort.Direction.ASC, sort = ["title"]) @ParameterObject pageable: Pageable,
        principal: Authentication,
    ): Page<BookDto> {
        assertIsJeluUser(principal.principal)
        return repository.findSeriesBooksById(seriesId, (principal.principal as JeluUser).user, pageable, libraryFilter ?: LibraryFilter.ANY)
    }

    @GetMapping(path = ["/authors/{id}"])
    fun authorById(@PathVariable("id") authorId: UUID): AuthorDto = repository.findAuthorsById(authorId)

    @GetMapping(path = ["/authors/{id}/books"])
    fun authorBooksById(
        @PathVariable("id") authorId: UUID,
        @RequestParam(name = "libraryFilter", required = false) libraryFilter: LibraryFilter?,
        @RequestParam(name = "roleFilter", required = false) roleFilter: Role?,
        @PageableDefault(page = 0, size = 20, direction = Sort.Direction.ASC, sort = ["title"]) @ParameterObject pageable: Pageable,
        principal: Authentication,
    ): Page<BookDto> =
        repository.findAuthorBooksById(authorId, (principal.principal as JeluUser).user, pageable, libraryFilter ?: LibraryFilter.ANY, roleFilter ?: Role.ANY)

    @PostMapping(path = ["/books"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun saveBook(
        @RequestBody @Valid
        book: BookCreateDto,
    ): BookDto {
        return repository.save(book, null)
    }

    @PostMapping(path = ["/books"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun saveBook(
        @RequestPart("book") @Valid book: BookCreateDto,
        @RequestPart("file", required = false) file: MultipartFile?,
    ): BookDto {
        return repository.save(book, file)
    }

    @PostMapping(path = ["/userbooks"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun saveUserBook(
        @RequestBody @Valid
        book: CreateUserBookDto,
        principal: Authentication,
    ): UserBookLightDto {
        return repository.save(book, (principal.principal as JeluUser).user, null)
    }

    @PostMapping(path = ["/userbooks"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun saveUserBook(
        @RequestPart("book") @Valid book: CreateUserBookDto,
        principal: Authentication,
        @RequestPart("file", required = false) file: MultipartFile?,
    ): UserBookLightDto {
        return repository.save(book, (principal.principal as JeluUser).user, file)
    }

    @PostMapping(path = ["/authors"])
    fun saveAuthor(
        @RequestBody @Valid
        author: AuthorDto,
    ): AuthorDto {
        return repository.save(author)
    }

    @PutMapping(path = ["/books/{id}"])
    fun updateBook(
        @PathVariable("id")
        bookId: UUID,
        @RequestBody
        @Valid
        book: BookUpdateDto,
    ): BookDto {
        return repository.update(bookId, book)
    }

    @PutMapping(path = ["/authors/{id}"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun updateAuthor(
        @PathVariable("id")
        authorId: UUID,
        @RequestBody
        @Valid
        author: AuthorUpdateDto,
    ): AuthorDto {
        return repository.updateAuthor(authorId, author)
    }

    @PutMapping(path = ["/authors/{id}"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateAuthor(
        @PathVariable("id") authorId: UUID,
        @RequestPart("author") @Valid author: AuthorUpdateDto,
        @RequestPart("file", required = false) file: MultipartFile?,
    ): AuthorDto {
        return repository.updateAuthor(authorId, author, file)
    }

    @PutMapping(path = ["/authors/{id}/merge/{otherId}"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun mergeAuthors(
        @PathVariable("id") authorId: UUID,
        @PathVariable("otherId") otherId: UUID,
        @RequestBody @Valid
        author: AuthorUpdateDto,
        principal: Authentication,
    ): AuthorDto {
        return repository.mergeAuthors(authorId, otherId, author, (principal.principal as JeluUser).user)
    }

    @PutMapping(path = ["/userbooks/{id}"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun updateUserBook(
        @PathVariable("id")
        userBookId: UUID,
        @RequestBody
        @Valid
        book: UserBookUpdateDto,
    ): UserBookLightDto {
        return repository.update(userBookId, book)
    }

    @PutMapping(path = ["/userbooks/{id}"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateUserBook(
        @PathVariable("id") userBookId: UUID,
        @RequestPart("book") @Valid book: UserBookUpdateDto,
        @RequestPart("file", required = false) file: MultipartFile?,
    ): UserBookLightDto {
        return repository.update(userBookId, book, file)
    }

    @PutMapping(path = ["/userbooks"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun bulkUpdateUserBooks(
        @RequestBody @Valid
        bulkUpdateDto: UserBookBulkUpdateDto,
    ): Int {
        return repository.bulkEditUserbooks(bulkUpdateDto)
    }

    @PutMapping(path = ["/series/{id}"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun updateSeries(
        @PathVariable("id")
        seriesId: UUID,
        @RequestBody
        @Valid
        seriesUpdate: SeriesUpdateDto,
        principal: Authentication,
    ): SeriesDto {
        assertIsJeluUser(principal.principal)
        return repository.updateSeries(seriesId, seriesUpdate, (principal.principal as JeluUser).user)
    }
}
