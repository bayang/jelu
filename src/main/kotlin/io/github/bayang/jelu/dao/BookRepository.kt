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

@Repository
class BookRepository(
    val readingEventRepository: ReadingEventRepository
) {

    fun findAll(title: String?, isbn10: String?, isbn13: String?, series: String?, pageable: Pageable, user: User, filter: LibraryFilter = LibraryFilter.ANY): Page<Book> {
        val booksWithSameIdAndUserHasUserbook = BookTable.join(UserBookTable, JoinType.LEFT)
            .slice(BookTable.id)
            .select { UserBookTable.book eq BookTable.id and (UserBookTable.user eq user.id) }
            .withDistinct()
        val query = BookTable.join(UserBookTable, JoinType.LEFT, onColumn = UserBookTable.book, otherColumn = BookTable.id)
            .slice(BookTable.columns)
            .selectAll()
            .withDistinct()

        title?.let {
            query.andWhere { BookTable.title like "%$title%" }
        }
        isbn10?.let {
            query.andWhere { BookTable.isbn10 eq isbn10 }
        }
        isbn13?.let {
            query.andWhere { BookTable.isbn13 eq isbn13 }
        }
        series?.let {
            query.andWhere { BookTable.series like "%$series%" }
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
        return Tag.find { TagTable.name like "%$name%" }.toList()
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
        if (!book.isbn10.isNullOrBlank()) {
            updated.isbn10 = book.isbn10.trim()
        }
        if (!book.isbn13.isNullOrBlank()) {
            updated.isbn13 = book.isbn13.trim()
        }
        if (book.pageCount != null) {
            updated.pageCount = book.pageCount
        }
        if (!book.publisher.isNullOrBlank()) {
            updated.publisher = book.publisher.trim()
        }
        if (!book.summary.isNullOrBlank()) {
            updated.summary = sanitizeHtml(book.summary)
        }
        // image must be set when saving file succeeds
        if (!book.publishedDate.isNullOrBlank()) {
            updated.publishedDate = book.publishedDate.trim()
        }
        if (!book.series.isNullOrBlank()) {
            updated.series = book.series.trim()
        }
        if (book.numberInSeries != null) {
            updated.numberInSeries = book.numberInSeries
        }
        if (!book.amazonId.isNullOrBlank()) {
            updated.amazonId = book.amazonId.trim()
        }
        if (!book.goodreadsId.isNullOrBlank()) {
            updated.goodreadsId = book.goodreadsId.trim()
        }
        if (!book.googleId.isNullOrBlank()) {
            updated.googleId = book.googleId.trim()
        }
        if (!book.librarythingId.isNullOrBlank()) {
            updated.librarythingId = book.librarythingId.trim()
        }
        if (!book.language.isNullOrBlank()) {
            updated.language = book.language.trim()
        }
        updated.modificationDate = nowInstant()
        val authorsList = mutableListOf<Author>()
        book.authors?.forEach {
            val authorEntity: Author? = findAuthorsByName(it.name.trim()).firstOrNull()
            if (authorEntity != null) {
                authorsList.add(authorEntity)
            } else {
                authorsList.add(save(it))
            }
        }
        if (authorsList.isNotEmpty()) {
            if (updated.authors.empty()) {
                updated.authors = SizedCollection(authorsList)
            } else {
                val existing = updated.authors.toMutableList()
                existing.addAll(authorsList)
                val merged: SizedCollection<Author> = SizedCollection(existing)
                updated.authors = merged
            }
        }
        val tagsList = mutableListOf<Tag>()
        book.tags?.forEach {
            val tagEntity: Tag? = findTagsByName(it.name.trim()).firstOrNull()
            if (tagEntity != null) {
                tagsList.add(tagEntity)
            } else {
                tagsList.add(save(it))
            }
        }
        if (tagsList.isNotEmpty()) {
            if (updated.tags.empty()) {
                updated.tags = SizedCollection(tagsList)
            } else {
                val existing = updated.tags.toMutableList()
                existing.addAll(tagsList)
                val merged: SizedCollection<Tag> = SizedCollection(existing)
                updated.tags = merged
            }
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
        if (!book.personalNotes.isNullOrBlank()) {
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
        if (!author.biography.isNullOrBlank()) {
            found.biography = author.biography.trim()
        }
        if (!author.dateOfDeath.isNullOrBlank()) {
            found.dateOfDeath = author.dateOfDeath.trim()
        }
        if (!author.dateOfBirth.isNullOrBlank()) {
            found.dateOfBirth = author.dateOfBirth.trim()
        }
        if (!author.image.isNullOrBlank()) {
            found.image = author.image.trim()
        }
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
        pageable: Pageable
    ): PageImpl<UserBook> {
        val query: Query = UserBookTable.join(BookTable, JoinType.LEFT).slice(UserBookTable.columns).selectAll()
            .andWhere { UserBookTable.user eq userID }
        if (eventTypes != null && eventTypes.isNotEmpty()) {
            query.andWhere { UserBookTable.lastReadingEvent inList eventTypes }
        }
        toRead?.let {
            query.andWhere { UserBookTable.toRead eq toRead }
        }
        val total = query.count()
        query.limit(pageable.pageSize, pageable.offset)
        val orders: Array<Pair<Expression<*>, SortOrder>> = parseSorts(pageable.sort, Pair(UserBookTable.lastReadingEventDate, SortOrder.DESC_NULLS_LAST), UserBookTable, BookTable)
        query.orderBy(*orders)
        return PageImpl(
            UserBook.wrapRows(query).toList(),
            pageable,
            total
        )
    }

    fun deleteUserBookById(userbookId: UUID) {
        val entity: UserBook = UserBook[userbookId]
        entity.delete()
    }

    fun deleteBookById(bookId: UUID) {
        Book[bookId].delete()
    }

    fun deleteTagFromBook(bookId: UUID, tagId: UUID) {
        BookTags.deleteWhere {
            BookTags.tag eq tagId and(BookTags.book eq bookId)
        }
    }

    fun deleteTagById(tagId: UUID) {
        Tag[tagId].delete()
    }

    fun deleteAuthorFromBook(bookId: UUID, authorId: UUID) {
        BookAuthors.deleteWhere {
            BookAuthors.book eq bookId and(BookAuthors.author eq authorId)
        }
    }

    fun deleteAuthorById(authorId: UUID) {
        Author[authorId].delete()
    }
}
