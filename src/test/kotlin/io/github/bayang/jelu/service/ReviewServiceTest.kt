package io.github.bayang.jelu.service

import io.github.bayang.jelu.bookDto
import io.github.bayang.jelu.dao.User
import io.github.bayang.jelu.dao.Visibility
import io.github.bayang.jelu.dto.BookDto
import io.github.bayang.jelu.dto.CreateReviewDto
import io.github.bayang.jelu.dto.CreateUserDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.dto.UpdateReviewDto
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Pageable
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReviewServiceTest(
    @Autowired private val userService: UserService,
    @Autowired private val bookService: BookService,
    @Autowired private val reviewService: ReviewService,
) {

    val reviewText = """
        |This is a review, I liked this book, because stuff.
        |I especially liked this thing on page 26
        |It was awesome
    """.trimMargin()

    @BeforeAll
    fun setupUser() {
        userService.save(CreateUserDto(login = "testuser", password = "1234", isAdmin = true))
    }

    @AfterAll
    fun teardDown() {
        reviewService.find(null, null, null, null, null, Pageable.ofSize(200))
            .forEach { reviewDto -> reviewService.delete(reviewDto.id!!) }
        userService.findAll(null).forEach { userService.deleteUser(it.id!!) }
    }

    @Test
    fun testLifeCycle() {
        val bookDto: BookDto = bookService.save(bookDto(), null)
        val createReviewDto = CreateReviewDto(
            null,
            reviewText,
            8.5,
            Visibility.PUBLIC,
            bookDto.id!!,
        )
        val saved = reviewService.save(
            createReviewDto,
            user(),
        )
        Assertions.assertNotNull(saved.id)
        Assertions.assertEquals(createReviewDto.visibility, saved.visibility)
        Assertions.assertEquals(createReviewDto.rating, saved.rating)
        Assertions.assertEquals(createReviewDto.text, saved.text)
        Assertions.assertNotNull(saved.reviewDate)
        val update = UpdateReviewDto(
            text = "new text",
            rating = 5.0,
            reviewDate = null,
            visibility = null,
        )
        val updated = reviewService.update(saved.id!!, update)
        Assertions.assertEquals(update.rating, updated.rating)
        Assertions.assertEquals(update.text, updated.text)
        Assertions.assertEquals(createReviewDto.visibility, updated.visibility)

        var found = reviewService.find(null, null, null, null, null, Pageable.ofSize(200))
        Assertions.assertEquals(1L, found.totalElements)
        val oneYearBefore = OffsetDateTime.now(ZoneId.systemDefault()).minusYears(1).toInstant()
        val createReviewDto1 = CreateReviewDto(
            oneYearBefore,
            reviewText,
            8.5,
            Visibility.PUBLIC,
            bookDto.id!!,
        )
        val saved1 = reviewService.save(
            createReviewDto1,
            user(),
        )
        Assertions.assertEquals(oneYearBefore, saved1.reviewDate)
        found = reviewService.find(null, null, null, null, null, Pageable.ofSize(200))
        Assertions.assertEquals(2L, found.totalElements)

        found = reviewService.find(null, null, null, LocalDate.ofInstant(oneYearBefore, ZoneId.systemDefault()).plusDays(1), null, Pageable.ofSize(200))
        Assertions.assertEquals(1L, found.totalElements)

        found = reviewService.find(null, null, null, null, LocalDate.ofInstant(oneYearBefore, ZoneId.systemDefault()).plusDays(1), Pageable.ofSize(200))
        Assertions.assertEquals(1L, found.totalElements)

        reviewService.delete(found.content[0].id!!)
        found = reviewService.find(null, null, null, null, null, Pageable.ofSize(200))
        Assertions.assertEquals(1L, found.totalElements)
    }

    fun user(): User {
        val userDetail = userService.loadUserByUsername("testuser")
        return (userDetail as JeluUser).user
    }
}
