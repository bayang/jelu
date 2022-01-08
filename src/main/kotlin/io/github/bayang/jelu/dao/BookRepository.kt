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
            query.map { resultRow -> Book.wrapRow(resultRow) },
            if (pageRequest.isPaged) PageRequest.of(pageRequest.pageNumber, pageRequest.pageSize, Sort.unsorted())
            else PageRequest.of(0, 20, Sort.unsorted()),
            total
        )
//        return query.map { resultRow -> Book.wrapRow(resultRow) }
    }

//    fun test(user: User): List<Book> {
//        val user = User[user.id.value]
//        val res =  BookTable.leftJoin(UserBookTable, { UserBookTable.book }, { BookTable.id }, additionalConstraint = { UserBookTable.user eq user.id })
//            .selectAll()
//            .map { resultRow -> Book.wrapRow(resultRow) }
//        res.forEach { book: Book -> displayBook(book) }
//        return res
//    }
//
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
        return Author.find{AuthorTable.name like "%$name%" }.toList()
    }

    fun findTagsByName(name: String): List<Tag> {
        return Tag.find{TagTable.name like "%$name%" }.toList()
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

    private fun displayRow(resultRow: ResultRow) {
        logger.debug { "row $resultRow" }
    }

    fun update(updated: Book, book: BookUpdateDto): Book {
        if (!book.title.isNullOrBlank()) {
            updated.title = book.title
        }
        book.isbn10.let { updated.isbn10 = it }
        book.isbn13.let { updated.isbn13 = it }
        book.pageCount.let { updated.pageCount = it }
        book.publisher.let { updated.publisher = it }
        book.summary.let { updated.summary = sanitizeHtml(it) }
        // image must be set when saving file succeeds
//        book.image.let { updated.image = it }
        book.publishedDate.let { updated.publishedDate = it }
        book.series.let { updated.series = it }
        book.numberInSeries.let { updated.numberInSeries = it }
        book.amazonId.let { updated.amazonId = it }
        book.goodreadsId.let { updated.goodreadsId = it }
        book.googleId.let { updated.googleId = it }
        book.librarythingId.let { updated.librarythingId = it }
        book.language.let { updated.language = it }
        updated.modificationDate = nowInstant()
        val authorsList = mutableListOf<Author>()
        book.authors?.forEach {
            val authorEntity: Author? = findAuthorsByName(it.name).firstOrNull()
            if (authorEntity != null) {
                authorsList.add(authorEntity)
            } else {
                authorsList.add(save(it))
            }
        }
        if (authorsList.isNotEmpty()) {
            if (updated.authors.empty()) {
                updated.authors = SizedCollection(authorsList)
            }
            else {
                val existing = updated.authors.toMutableList()
                existing.addAll(authorsList)
                val merged: SizedCollection<Author> = SizedCollection(existing)
                updated.authors = merged
            }
        }
        val tagsList = mutableListOf<Tag>()
        book.tags?.forEach {
            val tagEntity: Tag? = findTagsByName(it.name).firstOrNull()
            if (tagEntity != null) {
                tagsList.add(tagEntity)
            } else {
                tagsList.add(save(it))
            }
        }
        if (tagsList.isNotEmpty()) {
            if (updated.tags.empty()) {
                updated.tags = SizedCollection(tagsList)
            }
            else {
                val existing = updated.tags.toMutableList()
                existing.addAll(tagsList)
                val merged: SizedCollection<Tag> = SizedCollection(existing)
                updated.tags = merged
            }
        }
        return updated
    }

    fun update(bookId: UUID, book: BookUpdateDto): Book {
        var found: Book = Book[bookId]
        return update(found, book)
    }

    fun update(userBookId: UUID, book: UserBookUpdateDto): UserBook {
        var found: UserBook = UserBook[userBookId]
        if (book.owned != null) {
            found.owned = book.owned
        }
        if (!book.personalNotes.isNullOrBlank()) {
            found.personalNotes = book.personalNotes
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
            readingEventRepository.save(found, CreateReadingEventDto(
                eventType = book.lastReadingEvent,
                bookId = null
            ))
        }
        return found
    }

    fun updateAuthor(authorId: UUID, author: AuthorUpdateDto): Author {
        val found: Author = Author[authorId]
        author.name?.run { found.name = author.name }
        author.biography?.run { found.biography = author.biography }
        author.dateOfDeath?.run { found.dateOfDeath = author.dateOfDeath }
        author.dateOfBirth?.run { found.dateOfBirth = author.dateOfBirth }
        author.image?.run { found.image = author.image }
        found.modificationDate = nowInstant()
        return found
    }

    fun save(book: BookCreateDto): Book {
        val authorsList = mutableListOf<Author>()
        book.authors?.forEach { authorDto ->
            val authorEntity: Author? = findAuthorsByName(authorDto.name).firstOrNull()
            if (authorEntity != null) {
                authorsList.add(authorEntity)
            } else {
                authorsList.add(save(authorDto))
            }
        }
        val tagsList = mutableListOf<Tag>()
        book.tags?.forEach {
            val tagEntity: Tag? = findTagsByName(it.name).firstOrNull()
            if (tagEntity != null) {
                tagsList.add(tagEntity)
            } else {
                tagsList.add(save(it))
            }
        }
            val created = Book.new(UUID.randomUUID()){
                this.title = book.title
                val instant: Instant = nowInstant()
                this.creationDate = instant
                this.modificationDate = instant
                this.summary = sanitizeHtml(book.summary)
                this.isbn10 = book.isbn10
                this.isbn13 = book.isbn13
                this.pageCount = book.pageCount
                this.publishedDate = book.publishedDate
                this.publisher = book.publisher
                this.image = book.image
                this.series = book.series
                this.numberInSeries = book.numberInSeries
                this.amazonId = book.amazonId
                this.goodreadsId = book.goodreadsId
                this.googleId = book.googleId
                this.librarythingId = book.librarythingId
                this.language = book.language
            }

        created.authors = SizedCollection(authorsList)
        created.tags = SizedCollection(tagsList)
        created.load(Book::authors)
        created.load(Book::tags)
        return created
    }

    fun save(author: AuthorDto): Author {
        val created = Author.new{
            name = author.name
            image = author.image
            dateOfBirth = author.dateOfBirth
            dateOfDeath = author.dateOfDeath
            biography = author.biography
            val instant: Instant = nowInstant()
            creationDate = instant
            modificationDate = instant
        }
        return created
    }

    fun save(tag: TagDto): Tag {
        val created = Tag.new(UUID.randomUUID()){
            name = tag.name
            val instant: Instant = nowInstant()
            creationDate = instant
            modificationDate = instant
        }
        return created
    }

    fun save(book: Book, user: User, createUserBookDto: CreateUserBookDto): UserBook {
        val instant: Instant = nowInstant()
        return UserBook.new(UUID.randomUUID()){
            this.creationDate = instant
            this.modificationDate = instant
            this.user = user
            this.book = book
            this.owned = createUserBookDto.owned
            this.toRead = createUserBookDto.toRead
            this.personalNotes = createUserBookDto.personalNotes
            this.percentRead = createUserBookDto.percentRead
        }
    }

    fun findUserBookById(userbookId: UUID): UserBook = UserBook[userbookId]

    fun findUserBookByCriteria(userID: EntityID<UUID>, searchTerm: ReadingEventType?, toRead: Boolean?): List<UserBook> {
        return UserBook.find {
            val userFilter: Op<Boolean> = UserBookTable.user eq userID
            val eventFilter: Op<Boolean> = if (searchTerm != null) {
                UserBookTable.lastReadingEvent eq searchTerm
            }
            else {
                Op.TRUE
            }
            val toReadFilter: Op<Boolean> = if (toRead != null) {
                UserBookTable.toRead eq toRead
            }
            else {
                Op.TRUE
            }
            userFilter and eventFilter and toReadFilter
        }.orderBy(Pair(UserBookTable.lastReadingEventDate, SortOrder.DESC_NULLS_LAST))
            .toList()
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