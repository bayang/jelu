package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.AuthorDto
import io.github.bayang.jelu.dto.BookCreateDto
import io.github.bayang.jelu.dto.BookDto
import io.github.bayang.jelu.service.BookService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class BookDaoTests(@Autowired private val bookService: BookService) {

    @Test
    fun testInsertBooks() {
        val res: BookDto = bookService.save(
            BookCreateDto(
                id = null,
                title = "title1",
                isbn10 = "",
                isbn13 = "",
                summary = "",
                image = "",
                publisher = "",
                pageCount = 50,
                publishedDate = "",
                series = "",
                authors = emptyList(),
                numberInSeries = null,
                tags = emptyList(),
                goodreadsId = "",
                googleId = "",
                librarythingId = "",
                language = "",
                amazonId = ""
            ),
            null
        )
        Assertions.assertNotNull(res.id)
        val found = bookService.findBookById(res.id!!)
        Assertions.assertEquals(found.id, res.id)
        Assertions.assertEquals(found.authors, res.authors)
        Assertions.assertEquals(found.title, res.title)
        Assertions.assertEquals(found.isbn10, res.isbn10)
        Assertions.assertEquals(found.pageCount, res.pageCount)
    }

    @Test
    fun testInsertBooksWithAuthors() {
        var author = AuthorDto(id = null, creationDate = null, modificationDate = null, name = "test", image = "", dateOfBirth = "", dateOfDeath = "", biography = "")
        val res: BookDto = bookService.save(
            BookCreateDto(
                id = null,
                title = "title1",
                isbn10 = "",
                isbn13 = "",
                summary = "",
                image = "",
                publisher = "",
                pageCount = 50,
                publishedDate = "",
                series = "",
                authors = mutableListOf(author),
                numberInSeries = null,
                tags = emptyList(),
                goodreadsId = "",
                googleId = "",
                librarythingId = "",
                language = "",
                amazonId = ""
            ),
            null
        )
        Assertions.assertNotNull(res.id)
        val found = bookService.findBookById(res.id!!)
        Assertions.assertEquals(found.id, res.id)
        Assertions.assertEquals(found.authors?.get(0)?.name, res.authors?.get(0)?.name)
        Assertions.assertEquals(found.title, res.title)
        Assertions.assertEquals(found.isbn10, res.isbn10)
        Assertions.assertEquals(found.pageCount, res.pageCount)
    }
}