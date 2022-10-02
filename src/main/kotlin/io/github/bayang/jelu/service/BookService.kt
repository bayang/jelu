package io.github.bayang.jelu.service

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dao.Author
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
import io.github.bayang.jelu.dto.Role
import io.github.bayang.jelu.dto.TagDto
import io.github.bayang.jelu.dto.UserBookBulkUpdateDto
import io.github.bayang.jelu.dto.UserBookLightDto
import io.github.bayang.jelu.dto.UserBookUpdateDto
import io.github.bayang.jelu.dto.UserBookWithoutEventsAndUserDto
import io.github.bayang.jelu.dto.fromBookCreateDto
import io.github.bayang.jelu.service.metadata.FILE_PREFIX
import io.github.bayang.jelu.utils.imageName
import io.github.bayang.jelu.utils.resizeImage
import io.github.bayang.jelu.utils.slugify
import mu.KotlinLogging
import org.apache.commons.io.FilenameUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Component
class BookService(
    private val bookRepository: BookRepository,
    private val eventRepository: ReadingEventRepository,
    private val properties: JeluProperties,
    private val downloadService: DownloadService,
    private val fileManager: FileManager,
    private val shelfService: ShelfService
) {

    @Transactional
    fun findAll(
        title: String?,
        isbn10: String?,
        isbn13: String?,
        series: String?,
        authors: List<String>?,
        translators: List<String>?,
        tags: List<String>?,
        pageable: Pageable,
        user: User,
        libraryFilter: LibraryFilter
    ): Page<BookDto> =
        bookRepository.findAll(title, isbn10, isbn13, series, authors, translators, tags, pageable, user, libraryFilter).map { it.toBookDto() }

    @Transactional
    fun findAllAuthors(name: String?, pageable: Pageable): Page<AuthorDto> = bookRepository.findAllAuthors(name, pageable).map { it.toAuthorDto() }

    @Transactional
    fun findAllTags(name: String?, pageable: Pageable): Page<TagDto> = bookRepository.findAllTags(name, pageable).map { it.toTagDto() }

    @Transactional
    fun findBookById(bookId: UUID): BookDto = bookRepository.findBookById(bookId).toBookDto()

    @Transactional
    fun findAuthorsById(authorId: UUID): AuthorDto = bookRepository.findAuthorsById(authorId).toAuthorDto()

    /**
     * Image not updated, to add or update an image call the variant which accepts a MultiPartFile
     */
    @Transactional
    fun update(bookId: UUID, book: BookUpdateDto): BookDto = bookRepository.update(bookId, book).toBookDto()

    // call saveImages in case image url is set ?
    /**
     * Image not updated, to add or update an image call the variant which accepts a MultiPartFile
     */
    @Transactional
    fun update(userBookId: UUID, book: UserBookUpdateDto): UserBookLightDto = bookRepository.update(userBookId, book).toUserBookLightDto()

    @Transactional
    fun update(userBookId: UUID, book: UserBookUpdateDto, file: MultipartFile?): UserBookLightDto {
        val updated: UserBook = bookRepository.update(userBookId, book)
        val previousImage: String? = updated.book.image
        var backup: File? = null
        var skipSave = false
        // no multipart image and url image field is empty in udate dto
        // it means no new file upload and image has been explicitely set to null in update dto -> remove existing image
        // otherwise it is impossible to remove an image from the UI without replacing it by a new one
        if (file == null && book.book?.image.isNullOrBlank()) {
            skipSave = true
            // if only userbook is provided (eg if only userbook fields have to be updated)
            // then don't touch the image
            if (book.book != null && book.book.image.isNullOrBlank()) {
                updated.book.image = null
                if (previousImage != null) {
                    fileManager.deleteImage(previousImage)
                }
            }
        } else if (file == null && !book.book?.image.isNullOrBlank() && ! previousImage.isNullOrBlank() && previousImage.equals(book.book?.image, false)) {
            // no multipart file and image field in update dto is the same as in BDD -> no change
            skipSave = true
        }
        if (!skipSave) {
            // if we need to update image and there is already one, backup it
            if ((file != null || !book.book?.image.isNullOrBlank()) && !previousImage.isNullOrBlank()) {
                val currentImage = File(properties.files.images, previousImage)
                if (currentImage.exists()) {
                    backup = File(properties.files.images, "$previousImage.bak")
                    Files.move(currentImage.toPath(), backup.toPath())
                }
            }
            val savedImage: String? = saveImages(file, updated.book.title, updated.book.id.toString(), book.book?.image, properties.files.images)
            updated.book.image = savedImage
            // we had a previous image and we saved a new one : delete the old one
            if (backup != null && backup.exists() && savedImage != null && savedImage.isNotBlank()) {
                Files.deleteIfExists(backup.toPath())
            }
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
        var backup: File? = null
        var currentImage: File? = null
        if (file != null || userBook.book.image != null) {
            // existing book used on UserBook already had an image, backup it
            if (!book.image.isNullOrBlank()) {
                currentImage = File(properties.files.images, book.image)
                if (currentImage.exists()) {
                    backup = File(properties.files.images, "${book.image}.bak")
                    Files.move(currentImage.toPath(), backup.toPath())
                }
            }
            book.image = saveImages(file, book.title, book.id.toString(), userBook.book.image, properties.files.images)
            // we had a previous image and we saved a new one : delete the old one
            if (backup != null && backup.exists()) {
                // successfully saved new image, delete backup
                if (book.image != null && book.image!!.isNotBlank()) {
                    Files.deleteIfExists(backup.toPath())
                } else {
                    // saving new file failed ? Restore backup
                    if (currentImage != null) {
                        Files.move(backup.toPath(), currentImage.toPath())
                        book.image = currentImage.name
                    }
                }
            }
        }
        return created.toUserBookLightDto()
    }

    @Transactional
    fun save(book: BookCreateDto, file: MultipartFile?): BookDto {
        val saved: Book = bookRepository.save(book)
        saved.image = saveImages(file, saved.title, saved.id.toString(), book.image, properties.files.images)
        return saved.toBookDto()
    }

    fun saveImages(file: MultipartFile?, title: String, id: String, dtoImage: String?, targetDir: String): String? {
        var importedFile = false
        var savedImage: String? = null
        if (file != null) {
            try {
                val destFileName: String = imageName(slugify(title), id, FilenameUtils.getExtension(file.originalFilename))
                val destFile = File(targetDir, destFileName)
                logger.debug { "target import file at ${destFile.absolutePath}" }
                file.transferTo(destFile)
                importedFile = true
                savedImage = destFile.name
            } catch (e: Exception) {
                logger.error { "failed to save uploaded file ${file.originalFilename}" }
            }
        }

        if (! importedFile && ! dtoImage.isNullOrBlank()) {
            try {
                // file already exists in the right folder, just rename it
                if (dtoImage.startsWith(FILE_PREFIX)) {
                    val targetFilename: String = imageName(
                        slugify(title),
                        id, FilenameUtils.getExtension(dtoImage)
                    )
                    val currentFile = File(targetDir, "$dtoImage.bak")
                    val targetFile = File(currentFile.parent, targetFilename)
                    val succeeded = currentFile.renameTo(targetFile)
                    logger.debug { "renaming of metadata imported file $dtoImage was successful: $succeeded" }
                    savedImage = targetFilename
                } else {
                    val destFileName: String = downloadService.download(
                        dtoImage,
                        slugify(title),
                        id,
                        targetDir
                    )
                    savedImage = destFileName
                }
            } catch (e: Exception) {
                logger.error { "failed to save remote file ${file?.originalFilename}" }
            }
        }
        if (!savedImage.isNullOrBlank() && properties.files.resizeImages) {
            resizeImage(File(properties.files.images, savedImage))
        }
        return savedImage
    }

    @Transactional
    fun save(author: AuthorDto): AuthorDto = bookRepository.save(author).toAuthorDto()

    // call saveImages in case image url is set
    @Transactional
    fun updateAuthor(authorId: UUID, author: AuthorUpdateDto): AuthorDto = bookRepository.updateAuthor(authorId, author).toAuthorDto()

    @Transactional
    fun updateAuthor(authorId: UUID, author: AuthorUpdateDto, file: MultipartFile?): AuthorDto {
        var updated: Author = bookRepository.updateAuthor(authorId, author)
        // val updated: UserBook = bookRepository.update(userBookId, book)
        val previousImage: String? = updated.image
        var skipSave = false
        // no multipart image and url image field is empty in udate dto
        if (file == null && author.image.isNullOrBlank()) {
            skipSave = true
        } else if (file == null && !author.image.isNullOrBlank() && ! previousImage.isNullOrBlank() && previousImage.equals(author.image, false)) {
            // no multipart file and image field in update dto is the same as in BDD -> no change
            skipSave = true
        }
        // no new multipartFile and image field in update dto is the same as in bdd -> image has not changed, skip image saving
        if (! skipSave) {
            var backup: File? = null
            // if we need to update image and there is already one, backup it
            if ((file != null || !author.image.isNullOrBlank()) && !previousImage.isNullOrBlank()) {
                val currentImage = File(properties.files.images, previousImage)
                if (currentImage.exists()) {
                    backup = File(properties.files.images, "$previousImage.bak")
                    Files.move(currentImage.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            }
            val savedImage: String? = saveImages(file, updated.name, updated.id.toString(), author.image, properties.files.images)
            updated.image = savedImage
            // we had a previous image and we saved a new one : delete the old one
            if (backup != null && backup.exists() && !savedImage.isNullOrBlank()) {
                Files.deleteIfExists(backup.toPath())
            }
        }
        return updated.toAuthorDto()
    }

    @Transactional
    fun findUserBookById(userbookId: UUID): UserBookLightDto = bookRepository.findUserBookById(userbookId).toUserBookLightDto()

    @Transactional
    fun findUserBookByCriteria(
        userId: UUID,
        bookId: UUID?,
        eventTypes: List<ReadingEventType>?,
        toRead: Boolean?,
        owned: Boolean? = null,
        borrowed: Boolean? = null,
        pageable: Pageable
    ): Page<UserBookWithoutEventsAndUserDto> {
        return bookRepository.findUserBookByCriteria(userId, bookId, eventTypes, toRead, owned, borrowed, pageable).map { it.toUserBookWthoutEventsAndUserDto() }
    }

    @Transactional
    fun findTagById(tagId: UUID, user: User): TagDto {
        return bookRepository.findTagById(tagId).toTagDto()
    }

    @Transactional
    fun findTagBooksById(tagId: UUID, user: User, pageable: Pageable, libaryFilter: LibraryFilter): Page<BookDto> {
        return bookRepository.findTagBooksById(tagId, user, pageable, libaryFilter).map { book -> book.toBookDto() }
    }

    @Transactional
    fun save(tag: TagDto): TagDto {
        return bookRepository.save(tag).toTagDto()
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
    fun deleteTagsFromBook(bookId: UUID, tagIds: List<UUID>) {
        bookRepository.deleteTagsFromBook(bookId, tagIds)
    }

    @Transactional
    fun deleteTagById(tagId: UUID) {
        val shelves = shelfService.find(null, null, tagId)
        shelves.forEach {
            if (it.id != null) {
                try {
                    shelfService.delete(it.id)
                } catch (e: Exception) {
                    logger.debug { "failed to delete shelf ${it.name} while deleting corresponding tag" }
                }
            }
        }
        bookRepository.deleteTagById(tagId)
    }

    /**
     * Removes an author from a book without deleting the author from the database.
     * The author is removed only from that book.
     */
    @Transactional
    fun deleteAuthorFromBook(bookId: UUID, authorId: UUID) {
        bookRepository.deleteAuthorFromBook(bookId, authorId)
    }

    /**
     * Removes an translator from a book without deleting the translator from the database.
     * The translator is removed only from that book.
     */
    @Transactional
    fun deleteTranslatorFromBook(bookId: UUID, translatorId: UUID) {
        bookRepository.deleteTranslatorFromBook(bookId, translatorId)
    }

    @Transactional
    fun deleteAuthorById(authorId: UUID) {
        bookRepository.deleteAuthorById(authorId)
    }

    @Transactional
    fun findAuthorBooksById(authorId: UUID, user: User, pageable: Pageable, libaryFilter: LibraryFilter, role: Role = Role.ANY): Page<BookDto> {
        return bookRepository.findAuthorBooksById(authorId, user, pageable, libaryFilter, role).map { book -> book.toBookDto() }
    }

    @Transactional
    fun bulkEditUserbooks(userBookBulkUpdateDto: UserBookBulkUpdateDto): Int {
        return bookRepository.bulkEditUserbooks(userBookBulkUpdateDto)
    }

    @Transactional
    fun addTagsToBook(bookId: UUID, tagIds: List<UUID>): Int {
        return bookRepository.addTagsToBook(bookId, tagIds)
    }

    @Transactional
    fun mergeAuthors(authorId: UUID, otherId: UUID, authorUpdateDto: AuthorUpdateDto, user: User): AuthorDto {
        val pageNum = 0
        val size = 30
        val author = bookRepository.findAuthorsById(authorId)
        val authorToKeepDto = author.toAuthorDto()
        do {
            val booksPage: Page<Book> = bookRepository.findAuthorBooksById(otherId, user, PageRequest.of(pageNum, size))
            if (booksPage.hasContent()) {
                for (book in booksPage.content) {
                    val dto = book.toBookUpdateDto()
                    var filtered = dto.authors?.filter { authorDto -> authorDto.id != otherId }?.toMutableList()
                    if (filtered == null) {
                        filtered = mutableListOf()
                    }
                    if (!filtered.contains(authorToKeepDto)) {
                        filtered.add(authorToKeepDto)
                    }
                    dto.authors = filtered
                    var filteredTranslators = dto.translators?.filter { authorDto -> authorDto.id != otherId }?.toMutableList()
                    if (filteredTranslators == null) {
                        filteredTranslators = mutableListOf()
                    }
                    if (!filteredTranslators.contains(authorToKeepDto)) {
                        filteredTranslators.add(authorToKeepDto)
                    }
                    dto.translators = filteredTranslators
                    bookRepository.update(book, dto)
                }
            }
        } while (booksPage.hasNext())
        bookRepository.deleteAuthorById(otherId)
        return bookRepository.updateAuthor(authorId, authorUpdateDto).toAuthorDto()
    }
}
