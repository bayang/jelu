package io.github.bayang.jelu.service

import io.github.bayang.jelu.bookDto
import io.github.bayang.jelu.createUserBookDto
import io.github.bayang.jelu.dao.User
import io.github.bayang.jelu.dao.Visibility
import io.github.bayang.jelu.dto.BookDto
import io.github.bayang.jelu.dto.CreateReviewDto
import io.github.bayang.jelu.dto.CreateUserDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.dto.UpdateReviewDto
import io.github.bayang.jelu.dto.UserBookLightDto
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
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
        userService.save(CreateUserDto(login = "testuser1", password = "1234", isAdmin = false))
    }

    @AfterAll
    fun teardDown() {
        reviewService.find(null, null, null, null, null, Pageable.ofSize(200))
            .forEach { reviewDto -> reviewService.delete(reviewDto.id!!) }
        bookService.findUserBookByCriteria(user().id.value, null, null, null, null, null, Pageable.ofSize(30))
            .forEach { bookService.deleteUserBookById(it.id!!) }
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

    @Test
    fun testRatings() {
        val createBook = bookDto("title owned")
        val createUserBookDto = createUserBookDto(createBook)
        val saved: UserBookLightDto = bookService.save(createUserBookDto, user(), null)
        val createReviewDto = CreateReviewDto(
            null,
            reviewText,
            8.5,
            Visibility.PUBLIC,
            saved.book.id!!,
        )
        val savedReview = reviewService.save(
            createReviewDto,
            user(),
        )
        Assertions.assertNotNull(savedReview.id)
        val createReviewDto1 = CreateReviewDto(
            null,
            reviewText,
            1.5,
            Visibility.PUBLIC,
            saved.book.id!!,
        )
        val savedReview1 = reviewService.save(
            createReviewDto1,
            user("testuser1"),
        )
        val createReviewDto2 = CreateReviewDto(
            null,
            reviewText,
            1.5,
            Visibility.PUBLIC,
            saved.book.id!!,
        )
        val savedReview2 = reviewService.save(
            createReviewDto2,
            user(),
        )
        val userBookByCriteria = bookService.findUserBookByCriteria(
            user().id.value,
            null,
            null,
            null,
            null,
            null,
            PageRequest.ofSize(20),
        )
        // 2 reviews from our main user + 1 review from another user
        var dbl = (1.5 * 2 + 8.5) / 3
        dbl = Math.round(dbl * 100.0) / 100.0
        Assertions.assertEquals(dbl, userBookByCriteria.content[0].avgRating)
        Assertions.assertEquals(5.0, userBookByCriteria.content[0].userAvgRating)

        val createBook1 = bookDto("title owned 1")
        val createUserBookDto1 = createUserBookDto(createBook1)
        val saved1: UserBookLightDto = bookService.save(createUserBookDto1, user(), null)
        val createReviewDto3 = CreateReviewDto(
            null,
            reviewText,
            8.0,
            Visibility.PUBLIC,
            saved1.book.id!!,
        )
        val savedReview3 = reviewService.save(
            createReviewDto3,
            user(),
        )
        // another book with review from main user -> make sure we retrieve both, sorted by a rating field
        var userBookByCriteriaSorted = bookService.findUserBookByCriteria(
            user().id.value,
            null,
            null,
            null,
            null,
            null,
            PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "usrAvgRating")),
        )
        Assertions.assertEquals(2, userBookByCriteriaSorted.totalElements)
        Assertions.assertEquals(5.0, userBookByCriteriaSorted.content[0].userAvgRating)
        Assertions.assertEquals(8.0, userBookByCriteriaSorted.content[1].userAvgRating)
        userBookByCriteriaSorted = bookService.findUserBookByCriteria(
            user().id.value, null, null, null,
            null, null, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "usrAvgRating")),
        )
        Assertions.assertEquals(2, userBookByCriteriaSorted.totalElements)
        Assertions.assertEquals(8.0, userBookByCriteriaSorted.content[0].userAvgRating)
        Assertions.assertEquals(5.0, userBookByCriteriaSorted.content[1].userAvgRating)
        userBookByCriteriaSorted.content.forEach { ub ->
            if (ub.book.title == createBook1.title) {
                Assertions.assertEquals(8.0, ub.userAvgRating)
                Assertions.assertEquals(8.0, ub.avgRating)
            } else {
                Assertions.assertEquals(5.0, ub.userAvgRating)
                Assertions.assertEquals(dbl, ub.avgRating)
            }
        }

        val createBook2 = bookDto("title owned 2")
        val createUserBookDto2 = createUserBookDto(createBook2)
        val saved2: UserBookLightDto = bookService.save(createUserBookDto2, user(), null)
        // create a new book with no reviews, make sure it is retrieved as well
        userBookByCriteriaSorted = bookService.findUserBookByCriteria(
            user().id.value, null, null, null,
            null, null, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "usrAvgRating")),
        )
        Assertions.assertEquals(3, userBookByCriteriaSorted.totalElements)
        Assertions.assertEquals(8.0, userBookByCriteriaSorted.content[0].userAvgRating)
        Assertions.assertEquals(5.0, userBookByCriteriaSorted.content[1].userAvgRating)
        Assertions.assertNull(userBookByCriteriaSorted.content[2].userAvgRating) // make sure null field is always last

        userBookByCriteriaSorted = bookService.findUserBookByCriteria(
            user().id.value, null, null, null,
            null, null, PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "usrAvgRating")),
        )
        Assertions.assertEquals(3, userBookByCriteriaSorted.totalElements)
        Assertions.assertEquals(5.0, userBookByCriteriaSorted.content[0].userAvgRating)
        Assertions.assertEquals(8.0, userBookByCriteriaSorted.content[1].userAvgRating)
        Assertions.assertNull(userBookByCriteriaSorted.content[2].userAvgRating) // make sure null field is always last
    }

    fun user(username: String = "testuser"): User {
        val userDetail = userService.loadUserByUsername(username)
        return (userDetail as JeluUser).user
    }
}
