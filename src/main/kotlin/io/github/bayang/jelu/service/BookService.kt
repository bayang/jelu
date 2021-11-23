package io.github.bayang.jelu.service

import io.github.bayang.jelu.dao.BookRepository
import io.github.bayang.jelu.dao.ReadingEvent
import io.github.bayang.jelu.dao.User
import io.github.bayang.jelu.dto.*
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Component
class BookService(private val bookRepository: BookRepository) {

    @Transactional
    fun findAll(searchTerm: String?): List<BookDto> = bookRepository.findAll(searchTerm).map { it.toBookDto() }

    @Transactional
    fun findAllAuthors(): List<AuthorDto> = bookRepository.findAllAuthors().map { it.toAuthorDto() }

    @Transactional
    fun findAuthorsByName(name: String): AuthorDto? = bookRepository.findAuthorsByName(name)?.toAuthorDto()

    @Transactional
    fun findBookById(bookId: UUID): BookDto = bookRepository.findBookById(bookId).toBookDto()

    @Transactional
    fun findBookByTitle(title: String): List<BookDto> = bookRepository.findBookByTitle(title).map { it.toBookDto() }

    @Transactional
    fun findAuthorsById(authorId: UUID): AuthorWithBooksDto = bookRepository.findAuthorsById(authorId).toAuthorWithBooksDto()

    @Transactional
    fun update(bookId: UUID, book: BookUpdateDto): BookDto = bookRepository.update(bookId, book).toBookDto()

    @Transactional
    fun save(book: BookDto): BookDto = bookRepository.save(book).toBookDto()

    @Transactional
    fun save(author: AuthorDto): AuthorDto = bookRepository.save(author).toAuthorDto()

    @Transactional
    fun findAllBooksByUser(user: User): List<BookDtoWithEvents> {
        var events: List<ReadingEvent> = bookRepository.findAllBooksByUser(user)
        // filtering by users here is shitty
        // the sql request gives events with unique books but
        // back reference from book to events gives all events for all users
        //FIXME try to achieve everything in sql
        // anyway impact on perf should be low as we will always keep 1 user only
        return events.map { it.book }.map { it.toBookWithReadingEventsDto(user) }
    }

    @Transactional
    fun updateAuthor(authorId: UUID, author: AuthorUpdateDto): AuthorDto = bookRepository.updateAuthor(authorId, author).toAuthorDto()
}