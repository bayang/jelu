package io.github.bayang.jelu.service

import com.github.slugify.Slugify
import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dao.Book
import io.github.bayang.jelu.dao.BookRepository
import io.github.bayang.jelu.dao.ReadingEvent
import io.github.bayang.jelu.dao.User
import io.github.bayang.jelu.dto.*
import io.github.bayang.jelu.utils.imageName
import mu.KotlinLogging
import org.apache.commons.io.FilenameUtils
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.util.*

private val logger = KotlinLogging.logger {}

@Component
class BookService(
    private val bookRepository: BookRepository,
    private val eventService: ReadingEventService,
    private val properties: JeluProperties,
    private val downloadService: DownloadService
    ) {

    private val slugify: Slugify = Slugify()

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

    fun saveInternal(book: CreateBookDto, user: User): Book {
        val saved: Book = bookRepository.save(book)
        if (book.readingEvent != null) {
            eventService.save(CreateReadingEventDto(
                bookId= saved.id.value,
                eventType= book.readingEvent
            ), user)
        }
        return saved
    }

    @Transactional
    fun save(book: CreateBookDto, user: User): BookDto = saveInternal(book, user).toBookDto()

    @Transactional
    fun save(book: CreateBookDto, user: User, file: MultipartFile?): BookDto {
        val saved: Book = saveInternal(book, user)
        var importedFile = false
        //FIXME resize image when saving (protect with a flag)
        if (file != null) {
            try {
                var destFileName: String = imageName(slugify.slugify(saved.title), saved.id.toString(), FilenameUtils.getExtension(file.originalFilename))
                var destFile = File(properties.files.dir, destFileName)
                logger.debug { "target import file at ${destFile.absolutePath}" }
                file.transferTo(destFile)
                saved.image = destFile.name
                importedFile = true
            }
            catch (e: Exception) {
                logger.error { "failed to save uploaded file ${file.originalFilename}" }
            }
        }

        if (! importedFile && ! book.image.isNullOrBlank()) {
            try {
                val destFileName: String = downloadService.download(
                    book.image,
                    slugify.slugify(saved.title),
                    saved.id.toString(),
                    properties.files.dir
                )
                saved.image = destFileName
            }
            catch (e: Exception) {
                logger.error { "failed to save remote file ${book.image}" }
                saved.image = null
            }
        }
        return saved.toBookDto()
    }

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