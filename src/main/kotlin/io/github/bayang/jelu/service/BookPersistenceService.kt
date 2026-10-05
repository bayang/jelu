package io.github.bayang.jelu.service

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dao.Author
import io.github.bayang.jelu.dao.Book
import io.github.bayang.jelu.dao.BookRepository
import io.github.bayang.jelu.dao.ReadingEventRepository
import io.github.bayang.jelu.dao.UserBook
import io.github.bayang.jelu.dto.AuthorDto
import io.github.bayang.jelu.dto.AuthorUpdateDto
import io.github.bayang.jelu.dto.BookCreateDto
import io.github.bayang.jelu.dto.BookDto
import io.github.bayang.jelu.dto.BookUpdateDto
import io.github.bayang.jelu.dto.CreateReadingEventDto
import io.github.bayang.jelu.dto.CreateUserBookDto
import io.github.bayang.jelu.dto.UserBookLightDto
import io.github.bayang.jelu.dto.UserBookUpdateDto
import io.github.bayang.jelu.dto.UserDto
import io.github.bayang.jelu.dto.fromBookCreateDto
import io.github.bayang.jelu.service.metadata.providers.CalibreMetadataProvider
import io.github.bayang.jelu.utils.imageName
import io.github.bayang.jelu.utils.resizeImage
import io.github.bayang.jelu.utils.slugify
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.io.FilenameUtils
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Transactional half of [BookService], holding the save and update methods that also
 * touch cover images. Everything here runs while the sqlite write lock is held, which is
 * why it has no http client of its own. Remote covers are fetched by [BookService] before
 * the transaction opens and arrive as `stagedCover`, an absolute path to a local file.
 */
@Component
class BookPersistenceService(
    private val bookRepository: BookRepository,
    private val eventRepository: ReadingEventRepository,
    private val properties: JeluProperties,
    private val fileManager: FileManager,
    private val searchIndexService: SearchIndexService,
) {
    @Transactional
    fun update(
        bookId: UUID,
        book: BookUpdateDto,
        stagedCover: String?,
    ): BookDto {
        val res = bookRepository.update(bookId, book)
        val previousImage: String? = res.image
        var backup: File? = null
        var skipSave = false
        // image field is empty in udate dto and previous book had an image
        // it means image has been explicitely set to null in update dto -> remove existing image
        // otherwise it is impossible to remove an image from the UI without replacing it by a new one
        if (book.image.isNullOrBlank()) {
            skipSave = true
            // remove previous image
            if (book.image.isNullOrBlank() && !previousImage.isNullOrBlank()) {
                res.image = null
                fileManager.deleteImage(previousImage)
            }
        } else if (
            book.image.isNotBlank() &&
            !previousImage.isNullOrBlank() &&
            previousImage.equals(book.image, false)
        ) {
            // image field in update dto is the same as in BDD -> no change
            skipSave = true
        }
        if (!skipSave) {
            // if we need to update image and there is already one, backup it
            if (!book.image.isNullOrBlank() && !previousImage.isNullOrBlank()) {
                val currentImage = File(properties.files.images, previousImage)
                if (currentImage.exists()) {
                    backup = File(properties.files.images, "$previousImage.bak")
                    Files.move(currentImage.toPath(), backup.toPath())
                }
            }
            val savedImage: String? =
                saveImages(null, res.title, res.id.toString(), stagedCover ?: book.image, properties.files.images)
            res.image = savedImage
            // we had a previous image and we saved a new one : delete the old one
            if (backup != null && backup.exists() && !savedImage.isNullOrBlank()) {
                Files.deleteIfExists(backup.toPath())
            }
        }
        searchIndexService.bookUpdated(res)
        return res.toBookDto()
    }

    @Transactional
    fun update(
        userBookId: UUID,
        book: UserBookUpdateDto,
        file: MultipartFile?,
        stagedCover: String?,
    ): UserBookLightDto {
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
        } else if (file == null &&
            !book.book?.image.isNullOrBlank() &&
            !previousImage.isNullOrBlank() &&
            previousImage.equals(book.book.image, false)
        ) {
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
            val savedImage: String? =
                saveImages(
                    file,
                    updated.book.title,
                    updated.book.id.toString(),
                    stagedCover ?: book.book?.image,
                    properties.files.images,
                )
            updated.book.image = savedImage
            // we had a previous image and we saved a new one : delete the old one
            if (backup != null && backup.exists() && !savedImage.isNullOrBlank()) {
                Files.deleteIfExists(backup.toPath())
            }
        }
        searchIndexService.bookUpdated(updated.book)
        return updated.toUserBookLightDto()
    }

    @Transactional
    fun save(
        userBook: CreateUserBookDto,
        user: UserDto,
        file: MultipartFile?,
        stagedCover: String?,
    ): UserBookLightDto {
        var newBook = false
        val book: Book =
            if (userBook.book.id != null) {
                bookRepository.update(userBook.book.id, fromBookCreateDto(userBook.book))
            } else {
                bookRepository.save(userBook.book).also { newBook = true }
            }
        val created: UserBook = bookRepository.save(book, user, userBook)
        if (userBook.lastReadingEvent != null) {
            eventRepository.save(
                created,
                CreateReadingEventDto(
                    eventType = userBook.lastReadingEvent,
                    bookId = null,
                    eventDate = userBook.lastReadingEventDate,
                    startDate = null,
                ),
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
            book.image =
                saveImages(
                    file,
                    book.title,
                    book.id.toString(),
                    stagedCover ?: userBook.book.image,
                    properties.files.images,
                )
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
        if (newBook) {
            searchIndexService.bookAdded(book)
        } else {
            searchIndexService.bookUpdated(book)
        }
        return created.toUserBookLightDto()
    }

    @Transactional
    fun save(
        book: BookCreateDto,
        file: MultipartFile?,
        stagedCover: String?,
    ): BookDto {
        val saved: Book = bookRepository.save(book)
        saved.image =
            saveImages(file, saved.title, saved.id.toString(), stagedCover ?: book.image, properties.files.images)
        searchIndexService.bookAdded(saved)
        return saved.toBookDto()
    }

    @Transactional
    fun updateAuthor(
        authorId: UUID,
        author: AuthorUpdateDto,
        file: MultipartFile?,
        stagedCover: String?,
    ): AuthorDto {
        var updated: Author = bookRepository.updateAuthor(authorId, author)
        val previousImage: String? = updated.image
        var skipSave = false
        // no multipart image and url image field is empty in udate dto
        if (file == null && author.image.isNullOrBlank()) {
            skipSave = true
        } else if (file == null &&
            !author.image.isNullOrBlank() &&
            !previousImage.isNullOrBlank() &&
            previousImage.equals(author.image, false)
        ) {
            // no multipart file and image field in update dto is the same as in BDD -> no change
            skipSave = true
        }
        // no new multipartFile and image field in update dto is the same as in bdd -> image has not changed, skip image saving
        if (!skipSave) {
            var backup: File? = null
            // if we need to update image and there is already one, backup it
            if ((file != null || !author.image.isNullOrBlank()) && !previousImage.isNullOrBlank()) {
                val currentImage = File(properties.files.images, previousImage)
                if (currentImage.exists()) {
                    backup = File(properties.files.images, "$previousImage.bak")
                    Files.move(currentImage.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            }
            val savedImage: String? =
                saveImages(file, updated.name, updated.id.toString(), stagedCover ?: author.image, properties.files.images)
            updated.image = savedImage
            // we had a previous image and we saved a new one : delete the old one
            if (backup != null && backup.exists() && !savedImage.isNullOrBlank()) {
                Files.deleteIfExists(backup.toPath())
            }
        }
        searchIndexService.authorUpdated(authorId)
        return updated.toAuthorDto()
    }

    fun saveImages(
        file: MultipartFile?,
        title: String,
        id: String,
        dtoImage: String?,
        targetDir: String,
    ): String? {
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

        if (!importedFile && !dtoImage.isNullOrBlank()) {
            try {
                // file already exists in the right folder, just rename it
                if (dtoImage.startsWith(CalibreMetadataProvider.FILE_PREFIX)) {
                    val targetFilename: String =
                        imageName(
                            slugify(title),
                            id,
                            FilenameUtils.getExtension(dtoImage),
                        )
                    var currentFile = File(targetDir, "$dtoImage.bak")
                    if (!currentFile.exists()) {
                        currentFile = File(targetDir, dtoImage)
                    }
                    val targetFile = File(currentFile.parent, targetFilename)
                    val succeeded = currentFile.renameTo(targetFile)
                    logger.debug { "renaming of metadata imported file $dtoImage was successful: $succeeded" }
                    savedImage = targetFilename
                } else {
                    // file was picked on the server, or staged from a remote url by BookService
                    val file = File(dtoImage)
                    if (!file.exists() || !file.isAbsolute || file.isDirectory) {
                        logger.debug { "invalid file $dtoImage" }
                        return null
                    }
                    val targetFilename: String =
                        imageName(
                            slugify(title),
                            id,
                            FilenameUtils.getExtension(dtoImage),
                        )
                    val targetFile = File(targetDir, targetFilename)
                    // remote covers used to be streamed straight onto the target, which truncated
                    // whatever was there, so overwrite to keep that behaviour
                    file.copyTo(targetFile, overwrite = true)
                    savedImage = targetFilename
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
}
