package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.*
import io.github.bayang.jelu.utils.nowInstant
import org.jetbrains.exposed.sql.*
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
class BookRepository {

    fun findAll(searchTerm: String?): SizedIterable<Book> {
        return if (! searchTerm.isNullOrBlank()) {
            Book.find { BookTable.title like searchTerm }
        } else {
            Book.all()
        }
    }

    fun findAllBooksByUser(user: User): List<UserBook> {
//        return ReadingEvent
//            .find { ReadingEventTable.user eq user.id }
//            .distinctBy { it.book.id }
//            .map { it.book }
//    }

//    fun findAllBooksByUser(user: User): List<Book> {
//        val query = BookTable.innerJoin(ReadingEventTable).innerJoin(UserTable)
//            .slice(BookTable.columns)
//            .select {
//                ReadingEventTable.book as reb,
//                ReadingEventTable.user eq user.id
//            }
//            .withDistinct(true)
//        return Book.wrapRows(query).toList()
        return UserBook.find { UserBookTable.user eq user.id }
                        .orderBy(Pair(UserBookTable.modificationDate, SortOrder.DESC_NULLS_LAST))
                        .toList()

    }

    fun findAllAuthors(): SizedIterable<Author> = Author.all()

    fun findAuthorsByName(name: String): Author? {
        return Author.find{AuthorTable.name like name}.firstOrNull()
    }

    fun findBookById(bookId: UUID): Book = Book[bookId]

    fun findBookByTitle(title: String): SizedIterable<Book> = Book.find { BookTable.title like title }

    fun findAuthorsById(authorId: UUID): Author = Author[authorId]

    fun update(bookId: UUID, book: BookUpdateDto): Book {
        var found: Book = Book[bookId]
        if (!book.title.isNullOrBlank()) {
            found.title = book.title
        }
        book.isbn10.let { found.isbn10 = it }
        book.isbn13.let { found.isbn13 = it }
        book.pageCount.let { found.pageCount = it }
        book.publisher.let { found.publisher = it }
        book.summary.let { found.summary = it }
        book.image.let { found.image = it }
        book.publishedDate.let { found.publishedDate = it }
        found.modificationDate = nowInstant()
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
            if (found.authors.empty()) {
                found.authors = SizedCollection(authorsList)
            }
            else {
                val existing = found.authors.toMutableList()
                existing.addAll(authorsList)
                val merged: SizedCollection<Author> = SizedCollection(existing)
                found.authors = merged
            }
        }
        return found
    }

    fun updateAuthor(authorId: UUID, author: AuthorUpdateDto): Author {
        val found: Author = Author[authorId]
        author.name?.run { found.name = author.name }
        found.modificationDate = nowInstant()
        return found
    }

    fun save(book: CreateBookDto): Book {
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
            title = book.title
            val instant: Instant = nowInstant()
            creationDate = instant
            modificationDate = instant
            summary = book.summary
            isbn10 = book.isbn10
            isbn13 = book.isbn13
            pageCount = book.pageCount
            publishedDate = book.publishedDate
            publisher = book.publisher
            image = book.image
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
}