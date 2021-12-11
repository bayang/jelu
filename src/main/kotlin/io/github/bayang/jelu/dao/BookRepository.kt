package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.*
import io.github.bayang.jelu.utils.nowInstant
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
class BookRepository(
    val readingEventRepository: ReadingEventRepository
) {

    fun findAll(searchTerm: String?): SizedIterable<Book> {
        return if (! searchTerm.isNullOrBlank()) {
            Book.find { BookTable.title like searchTerm }
        } else {
            Book.all()
        }
    }

    fun findAllBooksByUser(user: User): List<UserBook> {
        return UserBook.find { UserBookTable.user eq user.id }
                .orderBy(Pair(UserBookTable.lastReadingEventDate, SortOrder.DESC_NULLS_LAST))
                .toList()
    }

    fun findAllAuthors(): SizedIterable<Author> = Author.all()

    fun findAuthorsByName(name: String): Author? {
        return Author.find{AuthorTable.name like name}.firstOrNull()
    }

    fun findBookById(bookId: UUID): Book = Book[bookId]

    fun findBookByTitle(title: String): SizedIterable<Book> = Book.find { BookTable.title like title }

    fun findAuthorsById(authorId: UUID): Author = Author[authorId]

    fun update(updated: Book, book: BookCreateDto): Book {
        if (!book.title.isNullOrBlank()) {
            updated.title = book.title
        }
        book.isbn10.let { updated.isbn10 = it }
        book.isbn13.let { updated.isbn13 = it }
        book.pageCount.let { updated.pageCount = it }
        book.publisher.let { updated.publisher = it }
        book.summary.let { updated.summary = it }
        book.image.let { updated.image = it }
        book.publishedDate.let { updated.publishedDate = it }
        book.series.let { updated.series = it }
        book.numberInSeries.let { updated.numberInSeries = it }
        updated.modificationDate = nowInstant()
        val authorsList = mutableListOf<Author>()
        book.authors?.forEach {
            val authorEntity: Author? = findAuthorsByName(it.name)
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
        return updated
    }

    fun update(bookId: UUID, book: BookCreateDto): Book {
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
        if (book.book != null) {
            update(found.book, book.book)
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
        found.modificationDate = nowInstant()
        return found
    }

    fun save(book: BookCreateDto): Book {
        val authorsList = mutableListOf<Author>()
        book.authors?.forEach {
            val authorEntity: Author? = findAuthorsByName(it.name)
            if (authorEntity != null) {
                authorsList.add(authorEntity)
            } else {
                authorsList.add(save(it))
            }
        }
        val created = Book.new{
            this.title = book.title
            val instant: Instant = nowInstant()
            this.creationDate = instant
            this.modificationDate = instant
            this.summary = book.summary
            this.isbn10 = book.isbn10
            this.isbn13 = book.isbn13
            this.pageCount = book.pageCount
            this.publishedDate = book.publishedDate
            this.publisher = book.publisher
            this.image = book.image
            this.series = book.series
            this.numberInSeries = book.numberInSeries
        }
        created.authors = SizedCollection(authorsList)
        return created
    }

    fun save(author: AuthorDto): Author {
        val created = Author.new{
            name = author.name
            val instant: Instant = nowInstant()
            creationDate = instant
            modificationDate = instant
        }
        return created
    }

    fun save(book: Book, user: User, createUserBookDto: CreateUserBookDto): UserBook {
        val instant: Instant = nowInstant()
        return UserBook.new {
            this.creationDate = instant
            this.modificationDate = instant
            this.user = user
            this.book = book
            this.owned = createUserBookDto.owned
            this.toRead = createUserBookDto.toRead
            this.personalNotes = createUserBookDto.personalNotes
        }
    }

    fun findUserBookById(userbookId: UUID): UserBook = UserBook[userbookId]

    fun findUserBookByLastEvent(userID: UUID, searchTerm: ReadingEventType): SizedIterable<UserBook> {
        return UserBook.find {
            UserBookTable.user eq userID and(UserBookTable.lastReadingEvent eq searchTerm)
        }
    }

    fun findUserBookByUserAndBook(user: User, book: Book): UserBook? {
        return UserBook.find { UserBookTable.user eq user.id and (UserBookTable.book.eq(book.id)) }.firstOrNull()
    }
}