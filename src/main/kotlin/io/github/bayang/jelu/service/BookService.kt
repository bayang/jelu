package io.github.bayang.jelu.service

import com.github.slugify.Slugify
import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dao.*
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
    private val eventRepository: ReadingEventRepository,
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
    fun update(bookId: UUID, book: BookCreateDto): BookDto = bookRepository.update(bookId, book).toBookDto()

    @Transactional
    fun update(userBookId: UUID, book: UserBookUpdateDto): UserBookLightDto = bookRepository.update(userBookId, book).toUserBookLightDto()

//    @Transactional
//    fun save(book: BookCreateDto): BookDto {
//        val saved: Book = bookRepository.save(book)
//        saveImages(null, saved, book, properties.files.dir)
//        return saved.toBookDto()
//    }

    @Transactional
    fun save(userBook: CreateUserBookDto, user: User, file: MultipartFile?): UserBookLightDto {
        val book: Book = if (userBook.book.id != null) {
            bookRepository.update(userBook.book.id, userBook.book)
        }
        else {
            bookRepository.save(userBook.book)
        }
        val created: UserBook = bookRepository.save(book, user, userBook)
        if (userBook.lastReadingEvent != null) {
            eventRepository.save(created, CreateReadingEventDto(
                eventType = userBook.lastReadingEvent,
                bookId = null
            ))
        }

        saveImages(file, book, userBook.book, properties.files.dir)
        return created.toUserBookLightDto()
    }

    @Transactional
    fun save(book: BookCreateDto, file: MultipartFile?): BookDto {
        val saved: Book = bookRepository.save(book)
        saveImages(file, saved, book, properties.files.dir)
        return saved.toBookDto()
    }

    fun saveImages(file: MultipartFile?, book: Book, bookDto: BookCreateDto, targetDir: String) {
        var importedFile = false
        //FIXME resize image when saving (protect with a flag)
        if (file != null) {
            try {
                var destFileName: String = imageName(slugify.slugify(book.title), book.id.toString(), FilenameUtils.getExtension(file.originalFilename))
                var destFile = File(targetDir, destFileName)
                logger.debug { "target import file at ${destFile.absolutePath}" }
                file.transferTo(destFile)
                book.image = destFile.name
                importedFile = true
            }
            catch (e: Exception) {
                logger.error { "failed to save uploaded file ${file.originalFilename}" }
            }
        }

        if (! importedFile && ! bookDto.image.isNullOrBlank()) {
            try {
                val destFileName: String = downloadService.download(
                    bookDto.image,
                    slugify.slugify(book.title),
                    book.id.toString(),
                    targetDir
                )
                book.image = destFileName
            }
            catch (e: Exception) {
                logger.error { "failed to save remote file ${book.image}" }
                book.image = null
            }
        }
    }

    @Transactional
    fun save(author: AuthorDto): AuthorDto = bookRepository.save(author).toAuthorDto()

    @Transactional
    fun findAllBooksByUser(user: User): List<UserBookLightDto> = bookRepository.findAllBooksByUser(user).map { it.toUserBookLightDto() }

    @Transactional
    fun updateAuthor(authorId: UUID, author: AuthorUpdateDto): AuthorDto = bookRepository.updateAuthor(authorId, author).toAuthorDto()

    @Transactional
    fun findUserBookById(userbookId: UUID): UserBookLightDto = bookRepository.findUserBookById(userbookId).toUserBookLightDto()

    @Transactional
    fun findUserBookByLastEvent(userId: UUID, eventType: ReadingEventType)
    = bookRepository.findUserBookByLastEvent(userId, eventType)
}