package io.github.bayang.jelu.service

import io.github.bayang.jelu.dao.Book
import io.github.bayang.jelu.dao.BookRepository
import io.github.bayang.jelu.dao.ReadingEventType
import io.github.bayang.jelu.dto.AuthorDto
import io.github.bayang.jelu.dto.AuthorUpdateDto
import io.github.bayang.jelu.dto.BookCreateDto
import io.github.bayang.jelu.dto.BookDto
import io.github.bayang.jelu.dto.BookUpdateDto
import io.github.bayang.jelu.dto.CreateSeriesRatingDto
import io.github.bayang.jelu.dto.CreateUserBookDto
import io.github.bayang.jelu.dto.LibraryFilter
import io.github.bayang.jelu.dto.ReadingEventTypeFilter
import io.github.bayang.jelu.dto.Role
import io.github.bayang.jelu.dto.SeriesCreateDto
import io.github.bayang.jelu.dto.SeriesDto
import io.github.bayang.jelu.dto.SeriesRatingDto
import io.github.bayang.jelu.dto.SeriesUpdateDto
import io.github.bayang.jelu.dto.TagDto
import io.github.bayang.jelu.dto.TotalsStatsDto
import io.github.bayang.jelu.dto.UserBookBulkUpdateDto
import io.github.bayang.jelu.dto.UserBookLightDto
import io.github.bayang.jelu.dto.UserBookUpdateDto
import io.github.bayang.jelu.dto.UserBookWithoutEventsAndUserDto
import io.github.bayang.jelu.dto.UserDto
import io.github.bayang.jelu.search.LuceneEntity
import io.github.bayang.jelu.search.LuceneHelper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.io.FileUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
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
    private val downloadService: DownloadService,
    private val shelfService: ShelfService,
    private val searchIndexService: SearchIndexService,
    private val luceneHelper: LuceneHelper,
    private val bookPersistenceService: BookPersistenceService,
) {
    @Transactional
    fun findAll(
        query: String?,
        pageable: Pageable,
        user: UserDto,
        eventTypes: List<ReadingEventTypeFilter>?,
        toRead: Boolean?,
        owned: Boolean?,
        borrowed: Boolean?,
        libraryFilter: LibraryFilter,
    ): Page<BookDto> {
        val entitiesIds = luceneHelper.searchEntitiesIds(query, LuceneEntity.Book)
        // we had a query but nothing matched, so don't return anything
        // for empty queries however, even if entitiesIds is empty, just return everything
        // and use other filters to narrow the results
        if (!query.isNullOrBlank() && entitiesIds.isNullOrEmpty()) {
            return PageImpl(
                listOf(),
                pageable,
                0,
            )
        } else {
            return bookRepository
                .findAll(
                    entitiesIds,
                    pageable,
                    user,
                    eventTypes,
                    toRead,
                    owned,
                    borrowed,
                    libraryFilter,
                ).map { it.toBookDto() }
        }
    }

    @Transactional
    fun findAll(
        title: String?,
        isbn10: String?,
        isbn13: String?,
        series: String?,
        authors: List<String>?,
        translators: List<String>?,
        narrators: List<String>?,
        tags: List<String>?,
        pageable: Pageable,
        user: UserDto,
        libraryFilter: LibraryFilter,
    ): Page<BookDto> =
        bookRepository.findAll(title, isbn10, isbn13, series, authors, translators, narrators, tags, pageable, user, libraryFilter).map {
            it.toBookDto()
        }

    @Transactional
    fun findAllAuthors(
        name: String?,
        role: Role = Role.ANY,
        pageable: Pageable,
    ): Page<AuthorDto> = bookRepository.findAllAuthors(name, role = role, pageable = pageable).map { it.toAuthorDto() }

    @Transactional
    fun findAllTags(
        name: String?,
        pageable: Pageable,
    ): Page<TagDto> = bookRepository.findAllTags(name, pageable).map { it.toTagDto() }

    @Transactional
    fun findAllSeries(
        name: String?,
        userId: UUID?,
        pageable: Pageable,
    ): Page<SeriesDto> = bookRepository.findAllSeries(name, userId, pageable).map { it.toSeriesDto() }

    @Transactional
    fun findBookById(bookId: UUID): BookDto = bookRepository.findBookById(bookId).toBookDto()

    @Transactional
    fun findAuthorsById(authorId: UUID): AuthorDto = bookRepository.findAuthorsById(authorId).toAuthorDto()

    @Transactional
    fun findPublishers(
        name: String?,
        pageable: Pageable,
    ): Page<String> = bookRepository.findAllPublishers(name, pageable)

    /**
     * Image not updated, to add or update an image call the variant which accepts a MultiPartFile
     */
    fun update(
        bookId: UUID,
        book: BookUpdateDto,
    ): BookDto =
        withStagedCover(book.image) { stagedCover ->
            bookPersistenceService.update(bookId, book, stagedCover)
        }

    // call saveImages in case image url is set ?

    /**
     * Image not updated, to add or update an image call the variant which accepts a MultiPartFile
     */
    @Transactional
    fun update(
        userBookId: UUID,
        book: UserBookUpdateDto,
    ): UserBookLightDto {
        val res = bookRepository.update(userBookId, book)
        searchIndexService.bookUpdated(res.book)
        return res.toUserBookLightDto()
    }

    fun update(
        userBookId: UUID,
        book: UserBookUpdateDto,
        file: MultipartFile?,
    ): UserBookLightDto =
        withStagedCover(book.book?.image, file) { stagedCover ->
            bookPersistenceService.update(userBookId, book, file, stagedCover)
        }

    fun save(
        userBook: CreateUserBookDto,
        user: UserDto,
        file: MultipartFile?,
    ): UserBookLightDto =
        withStagedCover(userBook.book.image, file) { stagedCover ->
            bookPersistenceService.save(userBook, user, file, stagedCover)
        }

    fun save(
        book: BookCreateDto,
        file: MultipartFile?,
    ): BookDto =
        withStagedCover(book.image, file) { stagedCover ->
            bookPersistenceService.save(book, file, stagedCover)
        }

    /**
     * Downloads a remote cover into a throwaway directory and hands the local path to
     * [block], so that a slow or hanging host can't stall us while the sqlite write lock
     * is held. Nothing is fetched when a [file] was uploaded, since it always wins over the
     * url, and anything that isn't an http url is passed through untouched for
     * [BookPersistenceService] to handle as before.
     */
    private fun <T> withStagedCover(
        image: String?,
        file: MultipartFile? = null,
        block: (String?) -> T,
    ): T {
        val url = image?.takeIf { file == null && isRemoteUrl(it) } ?: return block(null)
        // We own the whole directory rather than just the file, so that a download that
        // fails partway through doesn't leave anything behind either
        val stagingDir = Files.createTempDirectory("jelu-cover").toFile()
        try {
            return block(stageRemoteCover(url, stagingDir)?.absolutePath)
        } finally {
            FileUtils.deleteQuietly(stagingDir)
        }
    }

    private fun isRemoteUrl(image: String): Boolean = image.startsWith("http://", true) || image.startsWith("https://", true)

    private fun stageRemoteCover(
        url: String,
        stagingDir: File,
    ): File? =
        try {
            File(stagingDir, downloadService.download(url, "cover", UUID.randomUUID().toString(), stagingDir.absolutePath))
        } catch (e: Exception) {
            // a book that saves without its cover beats a save that fails outright, which is
            // what the image handling has always done with a broken download
            logger.error(e) { "failed to stage remote cover $url" }
            null
        }

    @Transactional
    fun save(author: AuthorDto): AuthorDto = bookRepository.save(author).toAuthorDto()

    // call saveImages in case image url is set
    @Transactional
    fun updateAuthor(
        authorId: UUID,
        author: AuthorUpdateDto,
    ): AuthorDto {
        val res = bookRepository.updateAuthor(authorId, author)
        searchIndexService.authorUpdated(authorId)
        return res.toAuthorDto()
    }

    fun updateAuthor(
        authorId: UUID,
        author: AuthorUpdateDto,
        file: MultipartFile?,
    ): AuthorDto =
        withStagedCover(author.image, file) { stagedCover ->
            bookPersistenceService.updateAuthor(authorId, author, file, stagedCover)
        }

    @Transactional
    fun updateSeries(
        seriesId: UUID,
        series: SeriesUpdateDto,
        user: UserDto,
    ): SeriesDto {
        val res = bookRepository.updateSeries(seriesId, series, user)
        searchIndexService.seriesUpdated(seriesId)
        return res.toSeriesDto()
    }

    @Transactional
    fun findUserBookById(userbookId: UUID): UserBookLightDto = bookRepository.findUserBookById(userbookId).toUserBookLightDto()

    @Transactional
    fun findUserBookByCriteria(
        userId: UUID,
        bookId: UUID?,
        eventTypes: List<ReadingEventTypeFilter>?,
        toRead: Boolean?,
        owned: Boolean? = null,
        borrowed: Boolean? = null,
        pageable: Pageable,
    ): Page<UserBookWithoutEventsAndUserDto> =
        bookRepository.findUserBookByCriteria(userId, bookId, eventTypes, toRead, owned, borrowed, pageable).map {
            it.toUserBookWthoutEventsAndUserDto()
        }

    @Transactional
    fun findOrphanTags(pageable: Pageable): Page<TagDto> = bookRepository.findOrphanTags(pageable).map { tag -> tag.toTagDto() }

    @Transactional
    fun findOrphanAuthors(pageable: Pageable): Page<AuthorDto> =
        bookRepository.findOrphanAuthors(pageable).map { author -> author.toAuthorDto() }

    @Transactional
    fun findOrphanSeries(pageable: Pageable): Page<SeriesDto> =
        bookRepository.findOrphanSeries(pageable).map { series -> series.toSeriesDto() }

    @Transactional
    fun findTagById(
        tagId: UUID,
        user: UserDto,
    ): TagDto = bookRepository.findTagById(tagId).toTagDto()

    @Transactional
    fun findTagBooksById(
        tagId: UUID,
        user: UserDto,
        pageable: Pageable,
        libaryFilter: LibraryFilter,
        eventTypes: List<ReadingEventType>?,
    ): Page<BookDto> = bookRepository.findTagBooksById(tagId, user, pageable, libaryFilter, eventTypes).map { book -> book.toBookDto() }

    @Transactional
    fun findSeriesBooksById(
        seriesId: UUID,
        user: UserDto,
        pageable: Pageable,
        libaryFilter: LibraryFilter,
    ): Page<BookDto> = bookRepository.findSeriesBooksById(seriesId, user, pageable, libaryFilter).map { book -> book.toBookDto() }

    @Transactional
    fun findSeriesById(seriesId: UUID): SeriesDto = bookRepository.findSeriesById(seriesId).toSeriesDto()

    @Transactional
    fun findSeriesById(
        seriesId: UUID,
        userId: UUID,
    ): SeriesDto = bookRepository.findSeriesById(seriesId, userId).toSeriesDto()

    @Transactional
    fun findSeriesRating(
        seriesId: UUID,
        userId: UUID,
    ): SeriesRatingDto? = bookRepository.findSeriesRating(seriesId, userId)?.toSeriesRatingDto()

    @Transactional
    fun save(tag: TagDto): TagDto = bookRepository.save(tag).toTagDto()

    @Transactional
    fun saveSeries(
        series: SeriesCreateDto,
        user: UserDto,
    ): SeriesDto = bookRepository.saveSeries(series, user).toSeriesDto()

    @Transactional
    fun deleteUserBookById(userbookId: UUID) {
        bookRepository.deleteUserBookById(userbookId)
    }

    @Transactional
    fun deleteBookById(bookId: UUID) {
        bookRepository.deleteBookById(bookId)
        searchIndexService.bookDeleted(bookId)
    }

    @Transactional
    fun deleteTagFromBook(
        bookId: UUID,
        tagId: UUID,
    ) {
        bookRepository.deleteTagFromBook(bookId, tagId)
        searchIndexService.bookUpdated(bookId)
    }

    @Transactional
    fun deleteTagsFromBook(
        bookId: UUID,
        tagIds: List<UUID>,
    ) {
        bookRepository.deleteTagsFromBook(bookId, tagIds)
        searchIndexService.bookUpdated(bookId)
    }

    @Transactional
    fun deleteTagById(tagId: UUID) {
        var shouldContinue = true
        do {
            val shelves = shelfService.find(null, null, tagId, Pageable.ofSize(100))
            if (shelves.totalElements > 0) {
                shelves.content.forEach { shelf ->
                    if (shelf.id != null) {
                        try {
                            shelfService.delete(shelf.id)
                        } catch (e: Exception) {
                            logger.debug { "failed to delete shelf ${shelf.name} while deleting corresponding tag" }
                        }
                    }
                }
            } else {
                shouldContinue = false
            }
        } while (shouldContinue)
        var books: Page<Book>
        val bookIds: MutableList<UUID> = mutableListOf()
        val pageSize = 30
        var pageNumber = 0
        do {
            books = bookRepository.findTagBooksByIdNoFilters(tagId, PageRequest.of(pageNumber, pageSize))
            books.forEach { bookIds.add(it.id.value) }
            pageNumber++
        }
        while (books.hasNext())
        bookRepository.deleteTagById(tagId)
        searchIndexService.booksUpdated(bookIds)
    }

    @Transactional
    fun deleteSeriesById(seriesId: UUID) {
        var books: Page<Book>
        val bookIds: MutableList<UUID> = mutableListOf()
        val pageSize = 30
        var pageNumber = 0
        do {
            books = bookRepository.findSeriesBooksByIdNoFilters(seriesId, PageRequest.of(pageNumber, pageSize))
            books.forEach { bookIds.add(it.id.value) }
            pageNumber++
        }
        while (books.hasNext())
        bookRepository.deleteSeriesById(seriesId)
        searchIndexService.booksUpdated(bookIds)
    }

    @Transactional
    fun deleteSeriesFromBook(
        bookId: UUID,
        seriesId: UUID,
    ) {
        bookRepository.deleteSeriesFromBook(bookId, seriesId)
        searchIndexService.bookUpdated(bookId)
    }

    /**
     * Removes an author from a book without deleting the author from the database.
     * The author is removed only from that book.
     */
    @Transactional
    fun deleteAuthorFromBook(
        bookId: UUID,
        authorId: UUID,
    ) {
        bookRepository.deleteAuthorFromBook(bookId, authorId)
        searchIndexService.bookUpdated(bookId)
    }

    /**
     * Removes an translator from a book without deleting the translator from the database.
     * The translator is removed only from that book.
     */
    @Transactional
    fun deleteTranslatorFromBook(
        bookId: UUID,
        translatorId: UUID,
    ) {
        bookRepository.deleteTranslatorFromBook(bookId, translatorId)
        searchIndexService.bookUpdated(bookId)
    }

    /**
     * Removes an narrator from a book without deleting the narrator from the database.
     * The narrator is removed only from that book.
     */
    @Transactional
    fun deleteNarratorFromBook(
        bookId: UUID,
        narratorId: UUID,
    ) {
        bookRepository.deleteNarratorFromBook(bookId, narratorId)
        searchIndexService.bookUpdated(bookId)
    }

    @Transactional
    fun deleteAuthorById(authorId: UUID) {
        var books: Page<Book>
        val bookIds: MutableList<UUID> = mutableListOf()
        val pageSize = 30
        var pageNumber = 0
        do {
            books = bookRepository.findAuthorBooksByIdNoFilters(authorId, PageRequest.of(pageNumber, pageSize))
            books.forEach { bookIds.add(it.id.value) }
            pageNumber++
        }
        while (books.hasNext())
        bookRepository.deleteAuthorById(authorId)
        searchIndexService.booksUpdated(bookIds)
    }

    @Transactional
    fun findAuthorBooksById(
        authorId: UUID,
        user: UserDto,
        pageable: Pageable,
        libaryFilter: LibraryFilter,
        role: Role = Role.ANY,
    ): Page<BookDto> = bookRepository.findAuthorBooksById(authorId, user, pageable, libaryFilter, role).map { book -> book.toBookDto() }

    @Transactional
    fun bulkEditUserbooks(userBookBulkUpdateDto: UserBookBulkUpdateDto): Int {
        val res = bookRepository.bulkEditUserbooks(userBookBulkUpdateDto)
        if (!userBookBulkUpdateDto.removeTags.isNullOrEmpty() || !userBookBulkUpdateDto.addTags.isNullOrEmpty()) {
            val bookIds =
                bookRepository.findUserBookByIdInList(userBookBulkUpdateDto.ids).map { ub -> ub.book.id.value }.toList()
            searchIndexService.booksUpdated(bookIds)
        }
        return res
    }

    @Transactional
    fun addTagsToBook(
        bookId: UUID,
        tagIds: List<UUID>,
    ): Int {
        val res = bookRepository.addTagsToBook(bookId, tagIds)
        searchIndexService.bookUpdated(bookId)
        return res
    }

    @Transactional
    fun save(
        seriesRatingDto: CreateSeriesRatingDto,
        user: UserDto,
    ): SeriesRatingDto = bookRepository.save(seriesRatingDto, user).toSeriesRatingDto()

    @Transactional
    fun mergeAuthors(
        authorId: UUID,
        otherId: UUID,
        authorUpdateDto: AuthorUpdateDto,
        user: UserDto,
    ): AuthorDto {
        val pageNum = 0
        val size = 30
        val author = bookRepository.findAuthorsById(authorId)
        val authorToKeepDto = author.toAuthorDto()
        val otherAuthorBooksIds: MutableList<UUID> = mutableListOf()
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

                    var filteredNarrators = dto.narrators?.filter { authorDto -> authorDto.id != otherId }?.toMutableList()
                    if (filteredNarrators == null) {
                        filteredNarrators = mutableListOf()
                    }
                    if (!filteredNarrators.contains(authorToKeepDto)) {
                        filteredNarrators.add(authorToKeepDto)
                    }
                    dto.narrators = filteredNarrators

                    bookRepository.update(book, dto)
                    otherAuthorBooksIds.add(book.id.value)
                }
            }
        } while (booksPage.hasNext())
        bookRepository.deleteAuthorById(otherId)
        searchIndexService.booksUpdated(otherAuthorBooksIds)
        val res = bookRepository.updateAuthor(authorId, authorUpdateDto)
        searchIndexService.authorUpdated(res.id.value)
        return res.toAuthorDto()
    }

    @Transactional
    fun migrateSeries() {
        var pageNum = 0
        val size = 30
        do {
            val booksPage: Page<Book> = bookRepository.booksWithSeries(PageRequest.of(pageNum, size))
            if (booksPage.hasContent()) {
                for (book in booksPage.content) {
                    bookRepository.updateSeriesFromStringToDedicatedTable(book)
                }
                pageNum++
            }
        } while (booksPage.hasNext())
    }

    @Transactional
    fun stats(userId: UUID): TotalsStatsDto = bookRepository.stats(userId).toTotalsStatsDto()

    @Transactional
    fun findBookUsersById(
        bookId: UUID,
        pageable: Pageable,
    ): Page<UserDto> = bookRepository.findBookUsersById(bookId, pageable).map { it.toUserDto() }
}
