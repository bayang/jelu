package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.*
import io.github.bayang.jelu.utils.nowInstant
import io.github.bayang.jelu.utils.sanitizeHtml
import mu.KotlinLogging
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

private val logger = KotlinLogging.logger {}

@Repository
class BookRepository(
    val readingEventRepository: ReadingEventRepository
) {

    fun findAll(title: String?, isbn10: String?, isbn13: String?, series: String?, page: Long = 0, pageSize: Long = 20): Page<Book> {
        val query: Query = BookTable.selectAll()
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
        val total = query.count()
        query.limit(pageSize.toInt(), page * pageSize)
        val pageRequest = PageRequest.of(page.toInt(), pageSize.toInt(), Sort.by(Sort.Order.desc("createdDate")))
        return PageImpl(
            Book.wrapRows(query).toList(),
            if (pageRequest.isPaged) PageRequest.of(pageRequest.pageNumber, pageRequest.pageSize, Sort.unsorted())
            else PageRequest.of(0, 20, Sort.unsorted()),
            total
        )
    }

//    private fun displayBook(book: Book) {
//        println(book.title)
//        book.userBooks.forEach { userBook: UserBook -> println(userBook.user.id) }
//    }

    fun findAllBooksByUser(user: User, page: Long, pageSize: Long): Page<UserBook> {
        val op: Op<Boolean> = UserBookTable.user eq user.id
        val total = UserBook.find { op }.count()
        val res = UserBook.find { op }.limit(pageSize.toInt(), page * pageSize)
        val pageRequest = PageRequest.of(page.toInt(), pageSize.toInt(), Sort.by(Sort.Order.desc("createdDate")))
        return PageImpl(
            res.toList(),
            if (pageRequest.isPaged) PageRequest.of(pageRequest.pageNumber, pageRequest.pageSize, Sort.unsorted())
            else PageRequest.of(0, 20, Sort.unsorted()),
            total
        )
//        return UserBook.find { UserBookTable.user eq user.id }
//            .limit(pageSize.toInt(), page * pageSize)
//                .orderBy(Pair(UserBookTable.lastReadingEventDate, SortOrder.DESC_NULLS_LAST))
//                .toList()
    }

    fun findAllAuthors(name: String?, page: Long = 0, pageSize: Long = 20): Page<Author> {
        val query: Query = AuthorTable.selectAll()
        name?.let {
            query.andWhere { AuthorTable.name like "%$name%" }
        }
        val total = query.count()
        query.limit(pageSize.toInt(), page * pageSize)
        val pageRequest = PageRequest.of(page.toInt(), pageSize.toInt(), Sort.by(Sort.Order.desc("createdDate")))
        return PageImpl(
            query.map { resultRow -> Author.wrapRow(resultRow) },
            if (pageRequest.isPaged) PageRequest.of(pageRequest.pageNumber, pageRequest.pageSize, Sort.unsorted())
            else PageRequest.of(0, 20, Sort.unsorted()),
            total
        )
    }

    fun findAllTags(): SizedIterable<Tag> = Tag.all()

    fun findAuthorsByName(name: String): List<Author> {
        return Author.find { AuthorTable.name like "%$name%" }.toList()
    }

    fun findTagsByName(name: String): List<Tag> {
        return Tag.find { TagTable.name like "%$name%" }.toList()
    }

    fun findBookById(bookId: UUID): Book = Book[bookId]

    fun findBookByTitle(title: String): SizedIterable<Book> = Book.find { BookTable.title like title }

    fun findAuthorsById(authorId: UUID): Author = Author[authorId]

    fun findTagById(tagId: UUID): Tag = Tag[tagId]

    fun findTagBooksById(tagId: UUID, page: Long, pageSize: Long): Page<Book> {
        val t = Tag[tagId]
        val query = BookTags.join(BookTable, JoinType.LEFT)
            .slice(BookTable.columns)
            .selectAll()
            .andWhere { BookTags.tag eq t.id }
        val total = query.count()
        query.limit(pageSize.toInt(), page * pageSize)
        val pageRequest = PageRequest.of(page.toInt(), pageSize.toInt(), Sort.by(Sort.Order.desc("createdDate")))
        return PageImpl(
            query.map { resultRow -> Book.wrapRow(resultRow) },
            if (pageRequest.isPaged) PageRequest.of(pageRequest.pageNumber, pageRequest.pageSize, Sort.unsorted())
            else PageRequest.of(0, 20, Sort.unsorted()),
            total
        )
    }

    fun displayRow(resultRow: ResultRow) {
        logger.debug { "row $resultRow" }
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
                    readDate = null
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
        userID: EntityID<UUID>,
        searchTerm: ReadingEventType?,
        toRead: Boolean?,
        page: Long,
        pageSize: Long
    ): PageImpl<UserBook> {
        val query: Query = UserBookTable.selectAll()
            .andWhere { UserBookTable.user eq userID }
        searchTerm?.let {
            query.andWhere { UserBookTable.lastReadingEvent eq searchTerm }
        }
        toRead?.let {
            query.andWhere { UserBookTable.toRead eq toRead }
        }

//        val query = UserBook.find {
//            val userFilter: Op<Boolean> = UserBookTable.user eq userID
//            val eventFilter: Op<Boolean> = if (searchTerm != null) {
//                UserBookTable.lastReadingEvent eq searchTerm
//            } else {
//                Op.TRUE
//            }
//            val toReadFilter: Op<Boolean> = if (toRead != null) {
//                UserBookTable.toRead eq toRead
//            } else {
//                Op.TRUE
//            }
//            userFilter and eventFilter and toReadFilter
//        }.orderBy(Pair(UserBookTable.lastReadingEventDate, SortOrder.DESC_NULLS_LAST))
        val total = query.count()
        query.limit(pageSize.toInt(), page * pageSize)
        query.orderBy(Pair(UserBookTable.lastReadingEventDate, SortOrder.DESC_NULLS_LAST))
        val pageRequest = PageRequest.of(page.toInt(), pageSize.toInt(), Sort.by(Sort.Order.desc("createdDate")))
        return PageImpl(
            UserBook.wrapRows(query).toList(),
            if (pageRequest.isPaged) PageRequest.of(pageRequest.pageNumber, pageRequest.pageSize, Sort.unsorted())
            else PageRequest.of(0, 20, Sort.unsorted()),
            total
        )

//        return UserBook.find {
//            val userFilter: Op<Boolean> = UserBookTable.user eq userID
//            val eventFilter: Op<Boolean> = if (searchTerm != null) {
//                UserBookTable.lastReadingEvent eq searchTerm
//            } else {
//                Op.TRUE
//            }
//            val toReadFilter: Op<Boolean> = if (toRead != null) {
//                UserBookTable.toRead eq toRead
//            } else {
//                Op.TRUE
//            }
//            userFilter and eventFilter and toReadFilter
//        }.orderBy(Pair(UserBookTable.lastReadingEventDate, SortOrder.DESC_NULLS_LAST))
//            .toList()
    }

    fun findUserBookByUserAndBook(user: User, book: Book): UserBook? {
        return UserBook.find { UserBookTable.user eq user.id and (UserBookTable.book.eq(book.id)) }.firstOrNull()
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

    fun findAuthorBooksById(authorId: UUID, page: Long, pageSize: Long): Page<Book> {
        val a = Author[authorId]
        val query = BookAuthors.join(BookTable, JoinType.LEFT)
            .slice(BookTable.columns)
            .selectAll()
            .andWhere { BookAuthors.author eq a.id }
        val total = query.count()
        query.limit(pageSize.toInt(), page * pageSize)
        val pageRequest = PageRequest.of(page.toInt(), pageSize.toInt(), Sort.by(Sort.Order.desc("createdDate")))
        return PageImpl(
            query.map { resultRow -> Book.wrapRow(resultRow) },
            if (pageRequest.isPaged) PageRequest.of(pageRequest.pageNumber, pageRequest.pageSize, Sort.unsorted())
            else PageRequest.of(0, 20, Sort.unsorted()),
            total
        )
    }
}
