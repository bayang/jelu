package io.github.bayang.jelu.service

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dao.*
import io.github.bayang.jelu.dto.*
import io.github.bayang.jelu.service.metadata.FILE_PREFIX
import io.github.bayang.jelu.utils.imageName
import io.github.bayang.jelu.utils.slugify
import mu.KotlinLogging
import org.apache.commons.io.FilenameUtils
import org.jetbrains.exposed.dao.id.EntityID
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.util.*

private val logger = KotlinLogging.logger {}

@Component
class BookService(
    private val bookRepository: BookRepository,
    private val eventRepository: ReadingEventRepository,
    private val properties: JeluProperties,
    private val downloadService: DownloadService,
) {

    @Transactional
    fun findAll(title: String?, isbn10: String?, isbn13: String?, series: String?, page: Long, pageSize: Long, user: User): Page<BookWithUserBookDto> =
        bookRepository.findAll(title, isbn10, isbn13, series, page, pageSize).map { it.toBookWithUserBookDto(user.id.value) }

    @Transactional
    fun findAllAuthors(name: String?, page: Long = 0, pageSize: Long = 20): Page<AuthorDto> = bookRepository.findAllAuthors(name, page, pageSize).map { it.toAuthorDto() }

    @Transactional
    fun findAllTags(): List<TagDto> = bookRepository.findAllTags().map { it.toTagDto() }

    @Transactional
    fun findAuthorsByName(name: String): List<AuthorDto> = bookRepository.findAuthorsByName(name).map { it.toAuthorDto() }

    @Transactional
    fun findTagsByName(name: String): List<TagDto> = bookRepository.findTagsByName(name).map { it.toTagDto() }

    @Transactional
    fun findBookById(bookId: UUID): BookDto = bookRepository.findBookById(bookId).toBookDto()

    @Transactional
    fun findBookByTitle(title: String): List<BookDto> = bookRepository.findBookByTitle(title).map { it.toBookDto() }

    @Transactional
    fun findAuthorsById(authorId: UUID): AuthorWithBooksDto = bookRepository.findAuthorsById(authorId).toAuthorWithBooksDto()

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
            val currentImage = File(properties.files.dir, previousImage)
            backup = File(properties.files.dir, "$previousImage.bak")
            Files.move(currentImage.toPath(), backup.toPath())
        }
        val savedImage: String = saveImages(file, updated.book, book.book, properties.files.dir)
        // we had a previous image and we saved a new one : delete the old one
        if (backup != null && backup.exists() && !savedImage.isNullOrBlank()) {
            Files.deleteIfExists(backup.toPath())
        }
        return updated.toUserBookLightDto()
    }

    @Transactional()
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
                    bookId = null
                )
            )
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

    fun saveImages(file: MultipartFile?, book: Book, bookDto: BookCreateDto?, targetDir: String): String {
        var importedFile = false
        var savedImage: String = ""
        // FIXME resize image when saving (protect with a flag)
        if (file != null) {
            try {
                var destFileName: String = imageName(slugify(book.title), book.id.toString(), FilenameUtils.getExtension(file.originalFilename))
                var destFile = File(targetDir, destFileName)
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
                // file already exists in the right folder, just rename it
                if (bookDto.image.startsWith(FILE_PREFIX)) {
                    val targetFilename: String = imageName(
                        slugify(book.title),
                        book.id.toString(), FilenameUtils.getExtension(bookDto.image)
                    )
                    val currentFile = File(targetDir, bookDto.image)
                    val targetFile = File(currentFile.parent, targetFilename)
                    val succeeded = currentFile.renameTo(targetFile)
                    logger.debug { "renaming of metadata imported file ${bookDto.image} was successful: $succeeded" }
                    book.image = targetFilename
                    savedImage = targetFilename
                } else {
                    val destFileName: String = downloadService.download(
                        bookDto.image,
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
    fun findAllBooksByUser(user: User, page: Long, pageSize: Long): Page<UserBookLightDto> =
        bookRepository.findAllBooksByUser(user, page, pageSize).map { it.toUserBookLightDto() }

    @Transactional
    fun updateAuthor(authorId: UUID, author: AuthorUpdateDto): AuthorDto = bookRepository.updateAuthor(authorId, author).toAuthorDto()

    @Transactional
    fun findUserBookById(userbookId: UUID): UserBookLightDto = bookRepository.findUserBookById(userbookId).toUserBookLightDto()

    @Transactional
    fun findUserBookByCriteria(
        userId: EntityID<UUID>,
        eventType: ReadingEventType?,
        toRead: Boolean?,
        page: Long,
        pageSize: Long
    ): Page<UserBookLightDto> =
        bookRepository.findUserBookByCriteria(userId, eventType, toRead, page, pageSize).map { it.toUserBookLightDto() }

    @Transactional
    fun findTagById(tagId: UUID, user: User): TagDto {
        return bookRepository.findTagById(tagId).toTagDto()
    }

    @Transactional
    fun findTagBooksById(tagId: UUID, user: User, page: Long, pageSize: Long): Page<BookWithUserBookDto> {
        return bookRepository.findTagBooksById(tagId, page, pageSize).map { book -> book.toBookWithUserBookDto(user.id.value) }
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
    fun findAuthorBooksById(authorId: UUID, user: User, page: Long, pageSize: Long): Page<BookWithUserBookDto> {
        return bookRepository.findAuthorBooksById(authorId, page, pageSize).map { book -> book.toBookWithUserBookDto(user.id.value) }
    }
}
