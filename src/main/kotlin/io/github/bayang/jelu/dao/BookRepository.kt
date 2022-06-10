package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.AuthorDto
import io.github.bayang.jelu.dto.AuthorUpdateDto
import io.github.bayang.jelu.dto.BookCreateDto
import io.github.bayang.jelu.dto.BookUpdateDto
import io.github.bayang.jelu.dto.CreateReadingEventDto
import io.github.bayang.jelu.dto.CreateUserBookDto
import io.github.bayang.jelu.dto.LibraryFilter
import io.github.bayang.jelu.dto.TagDto
import io.github.bayang.jelu.dto.UserBookUpdateDto
import io.github.bayang.jelu.dto.fromBookCreateDto
import io.github.bayang.jelu.service.FileManager
import io.github.bayang.jelu.utils.nowInstant
import io.github.bayang.jelu.utils.sanitizeHtml
import mu.KotlinLogging
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.orWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

fun parseSorts(sort: Sort, defaultSort: Pair<Expression<*>, SortOrder>, vararg tables: Table): Array<Pair<Expression<*>, SortOrder>> {
    val orders = mutableListOf<Pair<Expression<*>, SortOrder>>()
    val columns = mutableListOf<Column<*>>()
    for (table in tables) {
        columns.addAll(table.columns)
    }
    // will fail for common names in multiple tables like creationDate, so
    // put most important table first
    // eg if sort by creationDate should be applied on BookTable rather than UserBookTable put it first
    for (o in sort) {
        val found = columns.find { column -> column.name.replace("_", "", true).equals(o.property, true) }
        if (found != null) {
            orders.add(Pair(found, if (o.isAscending) SortOrder.ASC_NULLS_LAST else SortOrder.DESC_NULLS_LAST))
        }
    }
    if (orders.isEmpty()) {
        orders.add(defaultSort)
    }
    return orders.toTypedArray()
}

fun formatLike(input: String): String {
    return "%$input%"
}

@Repository
class BookRepository(
    val readingEventRepository: ReadingEventRepository,
    val fileManager: FileManager
) {

    fun findAll(title: String?, isbn10: String?, isbn13: String?, series: String?, authors: List<String>?, tags: List<String>?, pageable: Pageable, user: User, filter: LibraryFilter = LibraryFilter.ANY): Page<Book> {
        val booksWithSameIdAndUserHasUserbook = BookTable.join(UserBookTable, JoinType.LEFT)
            .slice(BookTable.id)
            .select { UserBookTable.book eq BookTable.id and (UserBookTable.user eq user.id) }
            .withDistinct()
        val query = BookTable.join(UserBookTable, JoinType.LEFT, onColumn = UserBookTable.book, otherColumn = BookTable.id)
            .join(BookAuthors, JoinType.LEFT, onColumn = BookTable.id, otherColumn = BookAuthors.book)
            .join(AuthorTable, JoinType.LEFT, onColumn = AuthorTable.id, otherColumn = BookAuthors.author)
            .join(BookTags, JoinType.LEFT, onColumn = BookTable.id, otherColumn = BookTags.book)
            .join(TagTable, JoinType.LEFT, onColumn = TagTable.id, otherColumn = BookTags.tag)
            .slice(BookTable.columns)
            .selectAll()
            .withDistinct()

        if (!title?.trim().isNullOrBlank()) {
            query.andWhere { BookTable.title like "%$title%" }
        }
        if (!isbn10?.trim().isNullOrBlank()) {
            query.andWhere { BookTable.isbn10 eq isbn10 }
        }
        if (!isbn13?.trim().isNullOrBlank()) {
            query.andWhere { BookTable.isbn13 eq isbn13 }
        }
        if (!series?.trim().isNullOrBlank()) {
            query.andWhere { BookTable.series like "%$series%" }
        }
        if (authors != null && authors.isNotEmpty()) {
            var first = true
            authors.forEach { author: String ->
                if (first) {
                    first = false
                    query.andWhere { AuthorTable.name like(formatLike(author)) }
                } else {
                    query.orWhere { AuthorTable.name like(formatLike(author)) }
                }
            }
        }
        if (tags != null && tags.isNotEmpty()) {
            var first = true
            tags.forEach { tag: String ->
                if (first) {
                    first = false
                    query.andWhere { TagTable.name like (formatLike(tag)) }
                } else {
                    query.orWhere { TagTable.name like (formatLike(tag)) }
                }
            }
        }
        if (filter == LibraryFilter.ONLY_USER_BOOKS) {
            // only books where user has an userbook
            query.andWhere { UserBookTable.user eq user.id }
        } else if (filter == LibraryFilter.ONLY_NON_USER_BOOKS) {
            // only books where there are no userbooks or only other users have userbooks
            query.andWhere { BookTable.id notInSubQuery booksWithSameIdAndUserHasUserbook }
        }

        val total = query.count()
        query.limit(pageable.pageSize, pageable.offset)
        val orders: Array<Pair<Expression<*>, SortOrder>> = parseSorts(pageable.sort, Pair(BookTable.title, SortOrder.ASC_NULLS_LAST), BookTable)
        query.orderBy(*orders)
        return PageImpl(
            query.map { resultRow -> wrapRow(resultRow, user.id.value) },
            pageable,
            total
        )
    }

    fun findAllAuthors(name: String?, pageable: Pageable): Page<Author> {
        val query: Query = AuthorTable.selectAll()
        name?.let {
            query.andWhere { AuthorTable.name like "%$name%" }
        }
        val total = query.count()
        query.limit(pageable.pageSize, pageable.offset)
        val orders: Array<Pair<Expression<*>, SortOrder>> = parseSorts(pageable.sort, Pair(AuthorTable.name, SortOrder.ASC_NULLS_LAST), AuthorTable)
        query.orderBy(*orders)
        return PageImpl(
            query.map { resultRow -> Author.wrapRow(resultRow) },
            pageable,
            total
        )
    }

    fun findAllTags(name: String?, pageable: Pageable): Page<Tag> {
        val query: Query = TagTable.selectAll()
        name?.let {
            query.andWhere { TagTable.name like "%$name%" }
        }
        val total = query.count()
        query.limit(pageable.pageSize, pageable.offset)
        val orders: Array<Pair<Expression<*>, SortOrder>> = parseSorts(pageable.sort, Pair(TagTable.name, SortOrder.ASC_NULLS_LAST), TagTable)
        query.orderBy(*orders)
        return PageImpl(
            query.map { resultRow -> Tag.wrapRow(resultRow) },
            pageable,
            total
        )
    }

    fun findAuthorsByName(name: String): List<Author> {
        return Author.find { AuthorTable.name like "%$name%" }.toList()
    }

    fun findTagsByName(name: String): List<Tag> {
        return Tag.find { TagTable.name.lowerCase() eq name.lowercase() }.toList()
    }

    fun findBookById(bookId: UUID): Book = Book[bookId]

    fun findAuthorsById(authorId: UUID): Author = Author[authorId]

    fun findTagById(tagId: UUID): Tag = Tag[tagId]

    fun findTagBooksById(tagId: UUID, user: User, pageable: Pageable, filter: LibraryFilter = LibraryFilter.ANY): Page<Book> {
        val booksWithSameIdAndUserHasUserbook = BookTable.join(UserBookTable, JoinType.LEFT)
            .slice(BookTable.id)
            .select { UserBookTable.book eq BookTable.id and (UserBookTable.user eq user.id) }
            .withDistinct()
        val query = BookTable.join(BookTags, JoinType.LEFT)
            .join(UserBookTable, JoinType.LEFT, onColumn = UserBookTable.book, otherColumn = BookTable.id)
            .slice(BookTable.columns)
            .select { BookTags.tag eq tagId }
        if (filter == LibraryFilter.ONLY_USER_BOOKS) {
            // only books where user has an userbook
            query.andWhere { UserBookTable.user eq user.id }
        } else if (filter == LibraryFilter.ONLY_NON_USER_BOOKS) {
            // only books where there are no userbooks or only other users have userbooks
            query.andWhere { BookTable.id notInSubQuery booksWithSameIdAndUserHasUserbook }
        }
        query.withDistinct(true)
        val total = query.count()
        query.limit(pageable.pageSize, pageable.offset)
        val orders: Array<Pair<Expression<*>, SortOrder>> = parseSorts(pageable.sort, Pair(BookTable.title, SortOrder.ASC_NULLS_LAST), BookTable)
        query.orderBy(*orders)
        return PageImpl(
            query.map { resultRow -> wrapRow(resultRow, user.id.value) },
            pageable,
            total
        )
    }

    fun findAuthorBooksById(authorId: UUID, user: User, pageable: Pageable, libaryFilter: LibraryFilter = LibraryFilter.ANY): Page<Book> {
        val booksWithSameIdAndUserHasUserbook = BookTable.join(UserBookTable, JoinType.LEFT)
            .slice(BookTable.id)
            .select { UserBookTable.book eq BookTable.id and (UserBookTable.user eq user.id) }
            .withDistinct()
        val query = BookTable.join(BookAuthors, JoinType.LEFT)
            .join(UserBookTable, JoinType.LEFT, onColumn = UserBookTable.book, otherColumn = BookTable.id)
            .slice(BookTable.columns)
            .select { BookAuthors.author eq authorId }
        if (libaryFilter == LibraryFilter.ONLY_USER_BOOKS) {
            // only books where user has an userbook
            query.andWhere { UserBookTable.user eq user.id }
        } else if (libaryFilter == LibraryFilter.ONLY_NON_USER_BOOKS) {
            // only books where there are no userbooks or only other users have userbooks
            query.andWhere { BookTable.id notInSubQuery booksWithSameIdAndUserHasUserbook }
        }
        query.withDistinct(true)
        val total = query.count()
        query.limit(pageable.pageSize, pageable.offset)
        val orders: Array<Pair<Expression<*>, SortOrder>> = parseSorts(pageable.sort, Pair(BookTable.title, SortOrder.ASC_NULLS_LAST), BookTable)
        query.orderBy(*orders)
        return PageImpl(
            query.map { resultRow -> wrapRow(resultRow, user.id.value) },
            pageable,
            total
        )
    }

    fun wrapRow(resultRow: ResultRow, userId: UUID): Book {
        val e = Book.wrapRow(resultRow)
        val userbook = e.userBooks.firstOrNull { u -> u.user.id.value == userId }
        if (userbook != null) {
            e.userBookId = userbook.id.value
        }
        return e
    }

    fun filterRow(resultRow: ResultRow): Boolean {
        displayRow(resultRow)
        return true
    }

    fun displayRow(resultRow: ResultRow) {
        logger.debug { "row $resultRow" }
        logger.debug { "${resultRow[BookTable.id]}  ${resultRow[UserBookTable.id]} ${resultRow[UserBookTable.user]}" }
    }

    fun update(updated: Book, book: BookUpdateDto): Book {
        if (!book.title.isNullOrBlank()) {
            updated.title = book.title.trim()
        }
        book.isbn10?.let {
            updated.isbn10 = book.isbn10.trim()
        }
        book.isbn13?.let {
            updated.isbn13 = book.isbn13.trim()
        }
        book.pageCount?.let {
            updated.pageCount = book.pageCount
        }
        book.publisher?.let {
            updated.publisher = book.publisher.trim()
        }
        if (!book.summary.isNullOrBlank()) {
            updated.summary = sanitizeHtml(book.summary)
        }
        // image must be set when saving file succeeds
        book.publishedDate?.let {
            updated.publishedDate = book.publishedDate.trim()
        }
        book.series?.let {
            updated.series = book.series.trim()
        }
        book.numberInSeries?.let {
            updated.numberInSeries = book.numberInSeries
        }
        book.amazonId?.let {
            updated.amazonId = book.amazonId.trim()
        }
        book.goodreadsId?.let {
            updated.goodreadsId = book.goodreadsId.trim()
        }
        book.googleId?.let {
            updated.googleId = book.googleId.trim()
        }
        book.librarythingId?.let {
            updated.librarythingId = book.librarythingId.trim()
        }
        book.language?.let {
            updated.language = book.language.trim()
        }
        updated.modificationDate = nowInstant()
        val authorsList = mutableListOf<Author>()
        book.authors?.forEach {
            // first try to find exact match by id
            var authorEntity: Author? = if (it.id != null) {
                findAuthorsById(it.id)
            }
            // if no id provided or research by id doesn't return anything try to find by name
            else {
                findAuthorsByName(it.name.trim()).firstOrNull()
            }
            if (authorEntity != null) {
                authorsList.add(authorEntity)
            } else {
                authorsList.add(save(it))
            }
        }
        if (authorsList.isNotEmpty()) {
            updated.authors = SizedCollection(authorsList)
        }
        val tagsList = mutableListOf<Tag>()
        book.tags?.forEach {
            // first try to find exact match by id
            val tagEntity: Tag? = if (it.id != null) {
                findTagById(it.id)
            }
            // if no id provided or research by id doesn't return anything try to find by name
            else {
                findTagsByName(it.name.trim()).firstOrNull()
            }
            if (tagEntity != null) {
                tagsList.add(tagEntity)
            } else {
                tagsList.add(save(it))
            }
        }
        if (tagsList.isNotEmpty()) {
            updated.tags = SizedCollection(tagsList)
        }
        return updated
    }

    fun update(bookId: UUID, book: BookUpdateDto): Book {
        val found: Book = Book[bookId]
        return update(found, book)
    }

    fun update(userBookId: UUID, book: UserBookUpdateDto): UserBook {
        val found: UserBook = UserBook[userBookId]
        if (book.owned != null) {
            found.owned = book.owned
        }
        book.personalNotes?.let {
            found.personalNotes = book.personalNotes.trim()
        }
        if (book.toRead != null) {
            found.toRead = book.toRead
        }
        if (book.percentRead != null) {
            found.percentRead = book.percentRead
        }
        if (book.book != null) {
            update(found.book, fromBookCreateDto(book.book))
        }
        if (book.lastReadingEvent != null) {
            readingEventRepository.save(
                found,
                CreateReadingEventDto(
                    eventType = book.lastReadingEvent,
                    bookId = null,
                    eventDate = null
                )
            )
        }
        return found
    }

    fun updateAuthor(authorId: UUID, author: AuthorUpdateDto): Author {
        val found: Author = Author[authorId]
        if (!author.name.isNullOrBlank()) {
            found.name = author.name.trim()
        }
        author.biography?.let {
            found.biography = author.biography.trim()
        }
        author.dateOfDeath?.let {
            found.dateOfDeath = author.dateOfDeath.trim()
        }
        author.dateOfBirth?.let {
            found.dateOfBirth = author.dateOfBirth.trim()
        }
        author.notes?.let {
            found.notes = author.notes.trim()
        }
        author.officialPage?.let {
            found.officialPage = author.officialPage.trim()
        }
        author.wikipediaPage?.let {
            found.wikipediaPage = author.wikipediaPage.trim()
        }
        author.goodreadsPage?.let {
            found.goodreadsPage = author.goodreadsPage.trim()
        }
        author.twitterPage?.let {
            found.twitterPage = author.twitterPage.trim()
        }
        author.facebookPage?.let {
            found.facebookPage = author.facebookPage.trim()
        }
        author.instagramPage?.let {
            found.instagramPage = author.instagramPage.trim()
        }
        // found.image = author.image?.trim()
        found.modificationDate = nowInstant()
        return found
    }

    fun save(book: BookCreateDto): Book {
        val authorsList = mutableListOf<Author>()
        book.authors?.forEach { authorDto ->
            val authorEntity: Author? = findAuthorsByName(authorDto.name.trim()).firstOrNull()
            if (authorEntity != null) {
                // we can receive the same author or the same but
                // with only one letter with a different case
                // so do not put twice the same entity in the list
                if (!authorsList.contains(authorEntity)) {
                    authorsList.add(authorEntity)
                }
            } else {
                authorsList.add(save(authorDto))
            }
        }
        val tagsList = mutableListOf<Tag>()
        book.tags?.forEach {
            val tagEntity: Tag? = findTagsByName(it.name.trim()).firstOrNull()
            if (tagEntity != null) {
                if (! tagsList.contains(tagEntity)) {
                    tagsList.add(tagEntity)
                }
            } else {
                tagsList.add(save(it))
            }
        }
        val created = Book.new(UUID.randomUUID()) {
            this.title = book.title.trim()
            val instant: Instant = nowInstant()
            this.creationDate = instant
            this.modificationDate = instant
            this.summary = sanitizeHtml(book.summary)
            this.isbn10 = cleanString(book.isbn10)
            this.isbn13 = cleanString(book.isbn13)
            this.pageCount = book.pageCount
            this.publishedDate = cleanString(book.publishedDate)
            this.publisher = cleanString(book.publisher)
            this.image = cleanString(book.image)
            this.series = cleanString(book.series)
            this.numberInSeries = book.numberInSeries
            this.amazonId = cleanString(book.amazonId)
            this.goodreadsId = cleanString(book.goodreadsId)
            this.googleId = cleanString(book.googleId)
            this.librarythingId = cleanString(book.librarythingId)
            this.language = cleanString(book.language)
        }

        created.authors = SizedCollection(authorsList)
        created.tags = SizedCollection(tagsList)
        // eager loading, see if we keep this in the long term
        created.load(Book::authors)
        created.load(Book::tags)
        return created
    }

    fun cleanString(input: String?): String? {
        if (input.isNullOrBlank()) {
            return null
        }
        return input.trim()
    }

    fun save(author: AuthorDto): Author {
        val created = Author.new {
            name = author.name.trim()
            image = cleanString(author.image)
            dateOfBirth = cleanString(author.dateOfBirth)
            dateOfDeath = cleanString(author.dateOfDeath)
            biography = cleanString(author.biography)
            val instant: Instant = nowInstant()
            creationDate = instant
            modificationDate = instant
            notes = cleanString(author.notes)
            officialPage = cleanString(author.officialPage)
            wikipediaPage = cleanString(author.wikipediaPage)
            goodreadsPage = cleanString(author.goodreadsPage)
            twitterPage = cleanString(author.twitterPage)
            facebookPage = cleanString(author.facebookPage)
            instagramPage = cleanString(author.instagramPage)
        }
        return created
    }

    fun save(tag: TagDto): Tag {
        val created = Tag.new(UUID.randomUUID()) {
            name = tag.name.trim()
            val instant: Instant = nowInstant()
            creationDate = instant
            modificationDate = instant
        }
        return created
    }

    fun save(book: Book, user: User, createUserBookDto: CreateUserBookDto): UserBook {
        val instant: Instant = nowInstant()
        return UserBook.new(UUID.randomUUID()) {
            this.creationDate = instant
            this.modificationDate = instant
            this.user = user
            this.book = book
            this.owned = createUserBookDto.owned
            this.toRead = createUserBookDto.toRead
            this.personalNotes = cleanString(createUserBookDto.personalNotes)
            this.percentRead = createUserBookDto.percentRead
        }
    }

    fun findUserBookById(userbookId: UUID): UserBook = UserBook[userbookId]

    fun findUserBookByCriteria(
        userID: UUID,
        eventTypes: List<ReadingEventType>?,
        toRead: Boolean?,
        owned: Boolean?,
        pageable: Pageable
    ): PageImpl<UserBook> {
        val cols = mutableListOf<Expression<*>>()
        cols.addAll(UserBookTable.columns)
        cols.addAll(BookTable.columns)
        val query: Query = UserBookTable.join(BookTable, JoinType.LEFT)
            .slice(cols).selectAll()
            .andWhere { UserBookTable.user eq userID }
            .withDistinct(true)
        if (eventTypes != null && eventTypes.isNotEmpty()) {
            query.andWhere { UserBookTable.lastReadingEvent inList eventTypes }
        }
        if (toRead != null) {
            if (toRead) {
                query.andWhere { UserBookTable.toRead eq toRead }
            } else {
                // default value if checkbox not set is null, so if caller asks explicitly with toRead == false,
                // try to return everything that is not true
                query.andWhere { UserBookTable.toRead eq toRead or (UserBookTable.toRead.isNull()) }
            }
        }
        if (owned != null) {
            if (owned) {
                query.andWhere { UserBookTable.owned eq owned }
            } else {
                // default value if checkbox not set is null, so if caller asks explicitly with owned == false,
                // try to return everything that is not true
                query.andWhere { UserBookTable.owned eq owned or (UserBookTable.owned.isNull()) }
            }
        }
        val total = query.count()
        query.limit(pageable.pageSize, pageable.offset)
        val orders: Array<Pair<Expression<*>, SortOrder>> = parseSorts(pageable.sort, Pair(UserBookTable.lastReadingEventDate, SortOrder.DESC_NULLS_LAST), UserBookTable, BookTable)
        query.orderBy(*orders)
        val res = UserBook.wrapRows(query).toList()
        return PageImpl(
            res,
            pageable,
            total
        )
    }

    fun deleteUserBookById(userbookId: UUID) {
        val entity: UserBook = UserBook[userbookId]
        entity.delete()
    }

    fun deleteBookById(bookId: UUID) {
        val book = Book[bookId]
        book.image?.let { fileManager.deleteImage(it) }
        book.delete()
    }

    fun deleteTagFromBook(bookId: UUID, tagId: UUID) {
        BookTags.deleteWhere {
            BookTags.tag eq tagId and(BookTags.book eq bookId)
        }
    }

    fun deleteTagById(tagId: UUID) {
        Tag[tagId].delete()
    }

    /**
     * Removes an author from a book without deleting the author from the database.
     * Used to clean the composite table
     */
    fun deleteAuthorFromBook(bookId: UUID, authorId: UUID) {
        BookAuthors.deleteWhere {
            BookAuthors.book eq bookId and(BookAuthors.author eq authorId)
        }
    }

    fun deleteAuthorById(authorId: UUID) {
        Author[authorId].delete()
    }
}
