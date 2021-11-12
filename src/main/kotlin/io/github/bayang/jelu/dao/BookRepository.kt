package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.AuthorDto
import io.github.bayang.jelu.dto.AuthorUpdateDto
import io.github.bayang.jelu.dto.BookDto
import io.github.bayang.jelu.dto.BookUpdateDto
import io.github.bayang.jelu.utils.nowInstant
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.SizedIterable
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

    fun findAllBooksByUser(user: User): List<Book> {
        return ReadingEvent
            .find { ReadingEventTable.user eq user.id }
            .distinctBy { it.book.id  }
            .map { it.book }
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
        book.publishedDate.let { found.publishedDate = it }
        found.modificationDate = nowInstant()
        return found
    }

    fun updateAuthor(authorId: UUID, author: AuthorUpdateDto): Author {
        val found: Author = Author[authorId]
        author.name?.run { found.name = author.name }
        found.modificationDate = nowInstant()
        return found
    }

    fun save(book: BookDto): Book {
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