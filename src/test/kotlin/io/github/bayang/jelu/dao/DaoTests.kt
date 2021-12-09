package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.BookCreateDto
import io.github.bayang.jelu.dto.BookDto
import io.github.bayang.jelu.service.BookService
import io.github.bayang.jelu.utils.nowInstant
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.util.*

@SpringBootTest
class DaoTests(@Autowired private val bookService: BookService) {

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
            numberInSeries = null
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
}