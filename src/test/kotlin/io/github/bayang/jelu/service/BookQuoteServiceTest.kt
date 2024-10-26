package io.github.bayang.jelu.service

import io.github.bayang.jelu.bookDto
import io.github.bayang.jelu.dao.Visibility
import io.github.bayang.jelu.dto.BookDto
import io.github.bayang.jelu.dto.CreateBookQuoteDto
import io.github.bayang.jelu.dto.CreateUserDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.dto.UpdateBookQuoteDto
import io.github.bayang.jelu.dto.UserDto
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Pageable

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BookQuoteServiceTest(
    @Autowired private val userService: UserService,
    @Autowired private val bookService: BookService,
    @Autowired private val bookQuoteService: BookQuoteService,
) {

    val quoteText = """
        |This is a review, I liked this book, because stuff.
        |I especially liked this thing on page 26
        |It was awesome
    """.trimMargin()

    @BeforeAll
    fun setupUser() {
        userService.save(CreateUserDto(login = "testuser", password = "1234", isAdmin = true))
        userService.save(CreateUserDto(login = "testuser1", password = "1234", isAdmin = false))
    }

    @AfterAll
    fun teardDown() {
        bookQuoteService.find(null, null, null, Pageable.ofSize(200))
            .forEach { quoteDto -> bookQuoteService.delete(quoteDto.id!!) }
        bookService.findUserBookByCriteria(user().id!!, null, null, null, null, null, Pageable.ofSize(30))
            .forEach { bookService.deleteUserBookById(it.id!!) }
        userService.findAll(null).forEach { userService.deleteUser(it.id!!) }
    }

    @Test
    fun testLifeCycle() {
        val bookDto: BookDto = bookService.save(bookDto(), null)
        val createBookQuoteDto = CreateBookQuoteDto(
            quoteText,
            Visibility.PUBLIC,
            bookDto.id!!,
            null,
        )
        val saved = bookQuoteService.save(
            createBookQuoteDto,
            user(),
        )
        Assertions.assertNotNull(saved.id)
        Assertions.assertEquals(createBookQuoteDto.visibility, saved.visibility)
        Assertions.assertEquals(createBookQuoteDto.text, saved.text)
        val update = UpdateBookQuoteDto(
            text = "new text",
            visibility = null,
            position = "page 225",
        )
        val updated = bookQuoteService.update(saved.id!!, update)
        Assertions.assertEquals(update.position, updated.position)
        Assertions.assertEquals(update.text, updated.text)
        Assertions.assertEquals(createBookQuoteDto.visibility, updated.visibility)

        var found = bookQuoteService.find(null, null, null, Pageable.ofSize(200))
        Assertions.assertEquals(1L, found.totalElements)
        bookQuoteService.delete(found.content[0].id!!)
        found = bookQuoteService.find(null, null, null, Pageable.ofSize(200))
        Assertions.assertEquals(0L, found.totalElements)
    }

    fun user(username: String = "testuser"): UserDto {
        val userDetail = userService.loadUserByUsername(username)
        return (userDetail as JeluUser).user
    }
}
