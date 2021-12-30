package io.github.bayang.jelu.service

import com.github.slugify.Slugify
import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dao.*
import io.github.bayang.jelu.dto.*
import io.github.bayang.jelu.utils.imageName
import mu.KotlinLogging
import org.apache.commons.io.FilenameUtils
import org.jetbrains.exposed.dao.id.EntityID
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

private val logger = KotlinLogging.logger {}

@Component
class BookService(
    private val bookRepository: BookRepository,
    private val eventRepository: ReadingEventRepository,
    private val properties: JeluProperties,
    private val downloadService: DownloadService,
    private val slugify: Slugify
    ) {

    @Transactional
    fun findAll(searchTerm: String?): List<BookDto> = bookRepository.findAll(searchTerm).map { it.toBookDto() }

    @Transactional
    fun findAllAuthors(): List<AuthorDto> = bookRepository.findAllAuthors().map { it.toAuthorDto() }

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
    fun update(bookId: UUID, book: BookCreateDto): BookDto = bookRepository.update(bookId, book).toBookDto()

    @Transactional
    fun update(userBookId: UUID, book: UserBookUpdateDto): UserBookLightDto = bookRepository.update(userBookId, book).toUserBookLightDto()

    @Transactional
    fun update(userBookId: UUID, book: UserBookUpdateDto, file: MultipartFile?): UserBookLightDto {
        val updated:UserBook = bookRepository.update(userBookId, book)
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

    @Transactional(rollbackFor = [Exception::class])
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

    fun saveImages(file: MultipartFile?, book: Book, bookDto: BookCreateDto?, targetDir: String): String {
        var importedFile = false
        var savedImage: String = ""
        //FIXME resize image when saving (protect with a flag)
        if (file != null) {
            try {
                var destFileName: String = imageName(slugify.slugify(book.title), book.id.toString(), FilenameUtils.getExtension(file.originalFilename))
                var destFile = File(targetDir, destFileName)
                logger.debug { "target import file at ${destFile.absolutePath}" }
                file.transferTo(destFile)
                book.image = destFile.name
                importedFile = true
                savedImage = destFile.name
            }
            catch (e: Exception) {
                logger.error { "failed to save uploaded file ${file.originalFilename}" }
            }
        }

        if (! importedFile && bookDto != null && ! bookDto.image.isNullOrBlank()) {
            try {
                // file already exists in the right folder, just rename it
                if (bookDto.image.startsWith(FILE_PREFIX)) {
                    val targetFilename: String = imageName(slugify.slugify(book.title),
                        book.id.toString(), FilenameUtils.getExtension(bookDto.image))
                    val currentFile = File(targetDir, bookDto.image)
                    val targetFile = File(currentFile.parent, targetFilename)
                    val succeeded = currentFile.renameTo(targetFile)
                    logger.debug { "renaming of metadata imported file ${bookDto.image} was successful: $succeeded" }
                    book.image = targetFilename
                    savedImage = targetFilename
                }
                else {
                    val destFileName: String = downloadService.download(
                        bookDto.image,
                        slugify.slugify(book.title),
                        book.id.toString(),
                        targetDir
                    )
                    book.image = destFileName
                    savedImage = destFileName
                }
            }
            catch (e: Exception) {
                logger.error { "failed to save remote file ${book.image}" }
                book.image = null
            }
        }
        return savedImage
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
    fun findUserBookByCriteria(userId: EntityID<UUID>, eventType: ReadingEventType?, toRead: Boolean?): List<UserBookLightDto>
    = bookRepository.findUserBookByCriteria(userId, eventType, toRead).map { it.toUserBookLightDto() }

    @Transactional
    fun findTagById(tagId: UUID, user: User): TagWithBooksDto {
        return bookRepository.findTagById(tagId).toTagWithBooksDto(user.id.value)
    }
}