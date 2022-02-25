package io.github.bayang.jelu.service

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dao.Book
import io.github.bayang.jelu.dao.BookRepository
import io.github.bayang.jelu.dao.ReadingEventRepository
import io.github.bayang.jelu.dao.ReadingEventType
import io.github.bayang.jelu.dao.User
import io.github.bayang.jelu.dao.UserBook
import io.github.bayang.jelu.dto.AuthorDto
import io.github.bayang.jelu.dto.AuthorUpdateDto
import io.github.bayang.jelu.dto.BookCreateDto
import io.github.bayang.jelu.dto.BookDto
import io.github.bayang.jelu.dto.BookUpdateDto
import io.github.bayang.jelu.dto.CreateReadingEventDto
import io.github.bayang.jelu.dto.CreateUserBookDto
import io.github.bayang.jelu.dto.LibraryFilter
import io.github.bayang.jelu.dto.TagDto
import io.github.bayang.jelu.dto.UserBookLightDto
import io.github.bayang.jelu.dto.UserBookUpdateDto
import io.github.bayang.jelu.dto.fromBookCreateDto
import io.github.bayang.jelu.service.metadata.FILE_PREFIX
import io.github.bayang.jelu.utils.imageName
import io.github.bayang.jelu.utils.slugify
import mu.KotlinLogging
import org.apache.commons.io.FilenameUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Component
class BookService(
    private val bookRepository: BookRepository,
    private val eventRepository: ReadingEventRepository,
    private val properties: JeluProperties,
    private val downloadService: DownloadService,
) {

    @Transactional
    fun findAll(
        title: String?,
        isbn10: String?,
        isbn13: String?,
        series: String?,
        authors: List<String>?,
        tags: List<String>?,
        pageable: Pageable,
        user: User,
        libraryFilter: LibraryFilter
    ): Page<BookDto> =
        bookRepository.findAll(title, isbn10, isbn13, series, authors, tags, pageable, user, libraryFilter).map { it.toBookDto() }

    @Transactional
    fun findAllAuthors(name: String?, pageable: Pageable): Page<AuthorDto> = bookRepository.findAllAuthors(name, pageable).map { it.toAuthorDto() }

    @Transactional
    fun findAllTags(name: String?, pageable: Pageable): Page<TagDto> = bookRepository.findAllTags(name, pageable).map { it.toTagDto() }

    @Transactional
    fun findBookById(bookId: UUID): BookDto = bookRepository.findBookById(bookId).toBookDto()

    @Transactional
    fun findAuthorsById(authorId: UUID): AuthorDto = bookRepository.findAuthorsById(authorId).toAuthorDto()

    @Transactional
    fun update(bookId: UUID, book: BookUpdateDto): BookDto = bookRepository.update(bookId, book).toBookDto()

    @Transactional
    fun update(userBookId: UUID, book: UserBookUpdateDto): UserBookLightDto = bookRepository.update(userBookId, book).toUserBookLightDto()

    @Transactional
    fun update(userBookId: UUID, book: UserBookUpdateDto, file: MultipartFile?): UserBookLightDto {
        val updated: UserBook = bookRepository.update(userBookId, book)
        val previousImage: String? = updated.book.image
        var backup: File? = null
        // if we need to update image and there is already one, backup it
        if ((file != null || !book.book?.image.isNullOrBlank()) && !previousImage.isNullOrBlank()) {
            val currentImage = File(properties.files.images, previousImage)
            backup = File(properties.files.images, "$previousImage.bak")
            Files.move(currentImage.toPath(), backup.toPath())
        }
        val savedImage: String = saveImages(file, updated.book, book.book, properties.files.images)
        // we had a previous image and we saved a new one : delete the old one
        if (backup != null && backup.exists() && savedImage.isNotBlank()) {
            Files.deleteIfExists(backup.toPath())
        }
        return updated.toUserBookLightDto()
    }

    @Transactional
    fun save(userBook: CreateUserBookDto, user: User, file: MultipartFile?): UserBookLightDto {
        val book: Book = if (userBook.book.id != null) {
            bookRepository.update(userBook.book.id, fromBookCreateDto(userBook.book))
        } else {
            bookRepository.save(userBook.book)
        }
        val created: UserBook = bookRepository.save(book, user, userBook)
        if (userBook.lastReadingEvent != null) {
            eventRepository.save(
                created,
                CreateReadingEventDto(
                    eventType = userBook.lastReadingEvent,
                    bookId = null,
                    eventDate = userBook.lastReadingEventDate
                )
            )
        }

        saveImages(file, book, userBook.book, properties.files.images)
        return created.toUserBookLightDto()
    }

    @Transactional
    fun save(book: BookCreateDto, file: MultipartFile?): BookDto {
        val saved: Book = bookRepository.save(book)
        saveImages(file, saved, book, properties.files.images)
        return saved.toBookDto()
    }

    fun saveImages(file: MultipartFile?, book: Book, bookDto: BookCreateDto?, targetDir: String): String {
        var importedFile = false
        var savedImage = ""
        // FIXME resize image when saving (protect with a flag)
        if (file != null) {
            try {
                val destFileName: String = imageName(slugify(book.title), book.id.toString(), FilenameUtils.getExtension(file.originalFilename))
                val destFile = File(targetDir, destFileName)
                logger.debug { "target import file at ${destFile.absolutePath}" }
                file.transferTo(destFile)
                book.image = destFile.name
                importedFile = true
                savedImage = destFile.name
            } catch (e: Exception) {
                logger.error { "failed to save uploaded file ${file.originalFilename}" }
            }
        }

        if (! importedFile && bookDto != null && ! bookDto.image.isNullOrBlank()) {
            try {
                val bookDtoImage = bookDto.image!!
                // file already exists in the right folder, just rename it
                if (bookDtoImage.startsWith(FILE_PREFIX)) {
                    val targetFilename: String = imageName(
                        slugify(book.title),
                        book.id.toString(), FilenameUtils.getExtension(bookDtoImage)
                    )
                    val currentFile = File(targetDir, bookDtoImage)
                    val targetFile = File(currentFile.parent, targetFilename)
                    val succeeded = currentFile.renameTo(targetFile)
                    logger.debug { "renaming of metadata imported file $bookDtoImage was successful: $succeeded" }
                    book.image = targetFilename
                    savedImage = targetFilename
                } else {
                    val destFileName: String = downloadService.download(
                        bookDtoImage,
                        slugify(book.title),
                        book.id.toString(),
                        targetDir
                    )
                    book.image = destFileName
                    savedImage = destFileName
                }
            } catch (e: Exception) {
                logger.error { "failed to save remote file ${book.image}" }
                book.image = null
            }
        }
        return savedImage
    }

    @Transactional
    fun save(author: AuthorDto): AuthorDto = bookRepository.save(author).toAuthorDto()

    @Transactional
    fun updateAuthor(authorId: UUID, author: AuthorUpdateDto): AuthorDto = bookRepository.updateAuthor(authorId, author).toAuthorDto()

    @Transactional
    fun findUserBookById(userbookId: UUID): UserBookLightDto = bookRepository.findUserBookById(userbookId).toUserBookLightDto()

    @Transactional
    fun findUserBookByCriteria(
        userId: UUID,
        eventTypes: List<ReadingEventType>?,
        toRead: Boolean?,
        pageable: Pageable
    ): Page<UserBookLightDto> =
        bookRepository.findUserBookByCriteria(userId, eventTypes, toRead, pageable).map { it.toUserBookLightDto() }

    @Transactional
    fun findTagById(tagId: UUID, user: User): TagDto {
        return bookRepository.findTagById(tagId).toTagDto()
    }

    @Transactional
    fun findTagBooksById(tagId: UUID, user: User, pageable: Pageable, libaryFilter: LibraryFilter): Page<BookDto> {
        return bookRepository.findTagBooksById(tagId, user, pageable, libaryFilter).map { book -> book.toBookDto() }
    }

    @Transactional
    fun deleteUserBookById(userbookId: UUID) {
        bookRepository.deleteUserBookById(userbookId)
    }

    @Transactional
    fun deleteBookById(bookId: UUID) {
        bookRepository.deleteBookById(bookId)
    }

    @Transactional
    fun deleteTagFromBook(bookId: UUID, tagId: UUID) {
        bookRepository.deleteTagFromBook(bookId, tagId)
    }

    @Transactional
    fun deleteTagById(tagId: UUID) {
        bookRepository.deleteTagById(tagId)
    }

    @Transactional
    fun deleteAuthorFromBook(bookId: UUID, authorId: UUID) {
        bookRepository.deleteAuthorFromBook(bookId, authorId)
    }

    @Transactional
    fun deleteAuthorById(authorId: UUID) {
        bookRepository.deleteAuthorById(authorId)
    }

    @Transactional
    fun findAuthorBooksById(authorId: UUID, user: User, pageable: Pageable, libaryFilter: LibraryFilter): Page<BookDto> {
        return bookRepository.findAuthorBooksById(authorId, user, pageable, libaryFilter).map { book -> book.toBookDto() }
    }
}
