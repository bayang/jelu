package io.github.bayang.jelu.service

import io.github.bayang.jelu.bookDto
import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.createUserBookDto
import io.github.bayang.jelu.dao.ReadingEventType
import io.github.bayang.jelu.dao.User
import io.github.bayang.jelu.dto.CreateReadingEventDto
import io.github.bayang.jelu.dto.CreateUserDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.dto.LibraryFilter
import io.github.bayang.jelu.dto.UpdateReadingEventDto
import io.github.bayang.jelu.dto.UserBookLightDto
import io.github.bayang.jelu.dto.UserBookUpdateDto
import io.github.bayang.jelu.errors.JeluException
import io.github.bayang.jelu.utils.nowInstant
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Pageable
import java.io.File
import java.time.temporal.ChronoUnit

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReadingEventServiceTest(
    @Autowired private val bookService: BookService,
    @Autowired private val userService: UserService,
    @Autowired private val jeluProperties: JeluProperties,
    @Autowired private val readingEventService: ReadingEventService
) {

    companion object {
        @TempDir
        lateinit var tempDir: File
    }

    @BeforeAll
    fun setupUser() {
        userService.save(CreateUserDto(login = "testuser", password = "1234", isAdmin = true))
        jeluProperties.files.images = tempDir.absolutePath
        println(jeluProperties.files.images)
    }

    @AfterAll
    fun teardDown() {
        userService.findAll(null).forEach { userService.deleteUser(it.id!!) }
    }

    @AfterEach
    fun cleanTest() {
        tempDir.listFiles().forEach {
            it.deleteRecursively()
        }
        readingEventService.findAll(null, null, Pageable.ofSize(30)).content.forEach {
            readingEventService.deleteReadingEventById(it.id!!)
        }
        bookService.findUserBookByCriteria(user().id.value, null, null, Pageable.ofSize(30))
            .forEach { bookService.deleteUserBookById(it.id!!) }
        bookService.findAllAuthors(null, Pageable.ofSize(30)).forEach {
            bookService.deleteAuthorById(it.id!!)
        }
        bookService.findAll(null, null, null, null, null, null, Pageable.ofSize(30), user(), LibraryFilter.ANY).forEach {
            bookService.deleteBookById(it.id!!)
        }
    }

    @Test
    fun testUpdateUserbookWithImageAndEventNoNewEventRequired() {
        val createBook = bookDto()
        val createUserBookDto = createUserBookDto(createBook, ReadingEventType.CURRENTLY_READING, nowInstant())
        val saved: UserBookLightDto = bookService.save(createUserBookDto, user(), null)
        Assertions.assertEquals(createBook.title, saved.book.title)
        Assertions.assertEquals(createBook.isbn10, saved.book.isbn10)
        Assertions.assertEquals(createBook.isbn13?.trim(), saved.book.isbn13)
        Assertions.assertEquals("This is a test summary with a newline", saved.book.summary)
        Assertions.assertEquals(createBook.publisher, saved.book.publisher)
        Assertions.assertEquals(createBook.pageCount, saved.book.pageCount)
        Assertions.assertEquals(createBook.goodreadsId, saved.book.goodreadsId)
        Assertions.assertNull(saved.book.librarythingId)
        Assertions.assertEquals(createUserBookDto.owned, saved.owned)
        Assertions.assertEquals(createUserBookDto.toRead, saved.toRead)
        Assertions.assertEquals(createUserBookDto.personalNotes, saved.personalNotes)
        Assertions.assertNull(saved.percentRead)
        Assertions.assertNotNull(saved.creationDate)
        Assertions.assertNotNull(saved.modificationDate)
        Assertions.assertNotNull(saved.book.creationDate)
        Assertions.assertNotNull(saved.book.modificationDate)
        Assertions.assertEquals(ReadingEventType.CURRENTLY_READING, saved.lastReadingEvent)
        Assertions.assertNotNull(saved.lastReadingEventDate)
        Assertions.assertEquals(1, readingEventService.findAll(null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)

        val updater = UserBookUpdateDto(
            ReadingEventType.DROPPED,
            personalNotes = "new notes",
            owned = false,
            book = null,
            toRead = null,
            percentRead = 50
        )
        val updated = bookService.update(saved.id!!, updater, null)
        Assertions.assertEquals(createBook.title, updated.book.title)
        Assertions.assertEquals(createBook.isbn10, updated.book.isbn10)
        Assertions.assertEquals(createBook.isbn13?.trim(), updated.book.isbn13)
        Assertions.assertEquals("This is a test summary with a newline", updated.book.summary)
        Assertions.assertEquals(createBook.publisher, updated.book.publisher)
        Assertions.assertEquals(createBook.pageCount, updated.book.pageCount)
        Assertions.assertEquals(createBook.goodreadsId, updated.book.goodreadsId)
        Assertions.assertNull(updated.book.librarythingId)
        Assertions.assertEquals(updater.owned, updated.owned)
        Assertions.assertEquals(saved.toRead, updated.toRead)
        Assertions.assertEquals(updater.percentRead, updated.percentRead)
        Assertions.assertEquals(updater.personalNotes, updated.personalNotes)
        Assertions.assertNotNull(updated.creationDate)
        Assertions.assertNotNull(updated.modificationDate)
        Assertions.assertNotNull(updated.book.creationDate)
        Assertions.assertNotNull(updated.book.modificationDate)
        Assertions.assertEquals(ReadingEventType.DROPPED, updated.lastReadingEvent)
        Assertions.assertNotNull(updated.lastReadingEventDate)
        Assertions.assertEquals(1, updated.readingEvents?.size)
        Assertions.assertEquals(1, readingEventService.findAll(null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
    }

    @Test
    fun testUpdateUserbookWithImageAndEventNewEventRequired() {
        val createBook = bookDto()
        val createUserBookDto = createUserBookDto(createBook, ReadingEventType.FINISHED, nowInstant())
        val saved: UserBookLightDto = bookService.save(createUserBookDto, user(), null)
        Assertions.assertEquals(createBook.title, saved.book.title)
        Assertions.assertEquals(createBook.isbn10, saved.book.isbn10)
        Assertions.assertEquals(createBook.isbn13?.trim(), saved.book.isbn13)
        Assertions.assertEquals("This is a test summary with a newline", saved.book.summary)
        Assertions.assertEquals(createBook.publisher, saved.book.publisher)
        Assertions.assertEquals(createBook.pageCount, saved.book.pageCount)
        Assertions.assertEquals(createBook.goodreadsId, saved.book.goodreadsId)
        Assertions.assertNull(saved.book.librarythingId)
        Assertions.assertEquals(createUserBookDto.owned, saved.owned)
        Assertions.assertEquals(createUserBookDto.toRead, saved.toRead)
        Assertions.assertEquals(createUserBookDto.personalNotes, saved.personalNotes)
        Assertions.assertNull(saved.percentRead)
        Assertions.assertNotNull(saved.creationDate)
        Assertions.assertNotNull(saved.modificationDate)
        Assertions.assertNotNull(saved.book.creationDate)
        Assertions.assertNotNull(saved.book.modificationDate)
        Assertions.assertEquals(ReadingEventType.FINISHED, saved.lastReadingEvent)
        Assertions.assertNotNull(saved.lastReadingEventDate)
        Assertions.assertEquals(1, readingEventService.findAll(null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)

        val updater = UserBookUpdateDto(
            ReadingEventType.DROPPED,
            personalNotes = "new notes",
            owned = false,
            book = null,
            toRead = null,
            percentRead = 50
        )
        val updated = bookService.update(saved.id!!, updater, null)
        Assertions.assertEquals(createBook.title, updated.book.title)
        Assertions.assertEquals(createBook.isbn10, updated.book.isbn10)
        Assertions.assertEquals(createBook.isbn13?.trim(), updated.book.isbn13)
        Assertions.assertEquals("This is a test summary with a newline", updated.book.summary)
        Assertions.assertEquals(createBook.publisher, updated.book.publisher)
        Assertions.assertEquals(createBook.pageCount, updated.book.pageCount)
        Assertions.assertEquals(createBook.goodreadsId, updated.book.goodreadsId)
        Assertions.assertNull(updated.book.librarythingId)
        Assertions.assertEquals(updater.owned, updated.owned)
        Assertions.assertEquals(saved.toRead, updated.toRead)
        Assertions.assertEquals(updater.percentRead, updated.percentRead)
        Assertions.assertEquals(updater.personalNotes, updated.personalNotes)
        Assertions.assertNotNull(updated.creationDate)
        Assertions.assertNotNull(updated.modificationDate)
        Assertions.assertNotNull(updated.book.creationDate)
        Assertions.assertNotNull(updated.book.modificationDate)
        Assertions.assertEquals(ReadingEventType.DROPPED, updated.lastReadingEvent)
        Assertions.assertNotNull(updated.lastReadingEventDate)
        Assertions.assertEquals(2, updated.readingEvents?.size)
        Assertions.assertEquals(2, readingEventService.findAll(null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
    }

    @Test
    fun testUpdateUserbookWithImageAndEventNewEventRequiredWithDateBefore() {
        val createBook = bookDto()
        val createUserBookDto = createUserBookDto(createBook, ReadingEventType.FINISHED, nowInstant())
        val saved: UserBookLightDto = bookService.save(createUserBookDto, user(), null)
        Assertions.assertEquals(createBook.title, saved.book.title)
        Assertions.assertEquals(createBook.isbn10, saved.book.isbn10)
        Assertions.assertEquals(createBook.isbn13?.trim(), saved.book.isbn13)
        Assertions.assertEquals("This is a test summary with a newline", saved.book.summary)
        Assertions.assertEquals(createBook.publisher, saved.book.publisher)
        Assertions.assertEquals(createBook.pageCount, saved.book.pageCount)
        Assertions.assertEquals(createBook.goodreadsId, saved.book.goodreadsId)
        Assertions.assertNull(saved.book.librarythingId)
        Assertions.assertEquals(createUserBookDto.owned, saved.owned)
        Assertions.assertEquals(createUserBookDto.toRead, saved.toRead)
        Assertions.assertEquals(createUserBookDto.personalNotes, saved.personalNotes)
        Assertions.assertNull(saved.percentRead)
        Assertions.assertNotNull(saved.creationDate)
        Assertions.assertNotNull(saved.modificationDate)
        Assertions.assertNotNull(saved.book.creationDate)
        Assertions.assertNotNull(saved.book.modificationDate)
        Assertions.assertEquals(ReadingEventType.FINISHED, saved.lastReadingEvent)
        Assertions.assertNotNull(saved.lastReadingEventDate)
        Assertions.assertEquals(1, readingEventService.findAll(null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)

        val thirtyDaysBefore = nowInstant().minus(30, ChronoUnit.DAYS)
        val newEvent = readingEventService.save(
            CreateReadingEventDto(
                ReadingEventType.CURRENTLY_READING,
                saved.book.id,
                thirtyDaysBefore
            ),
            user()
        )
        Assertions.assertEquals(ReadingEventType.CURRENTLY_READING, newEvent.eventType)

        var userbook = bookService.findUserBookById(saved.id!!)
        Assertions.assertEquals(ReadingEventType.FINISHED, userbook.lastReadingEvent)
        Assertions.assertTrue(userbook.lastReadingEventDate?.isAfter(nowInstant().minus(2, ChronoUnit.MINUTES))!!)

        Assertions.assertEquals(2, readingEventService.findAll(null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)

        readingEventService.deleteReadingEventById(newEvent.id!!)
        Assertions.assertEquals(1, readingEventService.findAll(null, null, Pageable.ofSize(30)).totalElements)
        userbook = bookService.findUserBookById(saved.id!!)
        Assertions.assertEquals(ReadingEventType.FINISHED, userbook.lastReadingEvent)
        Assertions.assertTrue(userbook.lastReadingEventDate?.isAfter(nowInstant().minus(2, ChronoUnit.MINUTES))!!)
    }

    @Test
    fun testUpdateUserbookWithImageAndEventNewEventRequiredWithDateAfter() {
        val createBook = bookDto()
        val createUserBookDto = createUserBookDto(createBook, ReadingEventType.FINISHED, nowInstant())
        val saved: UserBookLightDto = bookService.save(createUserBookDto, user(), null)
        Assertions.assertEquals(createBook.title, saved.book.title)
        Assertions.assertEquals(createBook.isbn10, saved.book.isbn10)
        Assertions.assertEquals(createBook.isbn13?.trim(), saved.book.isbn13)
        Assertions.assertEquals("This is a test summary with a newline", saved.book.summary)
        Assertions.assertEquals(createBook.publisher, saved.book.publisher)
        Assertions.assertEquals(createBook.pageCount, saved.book.pageCount)
        Assertions.assertEquals(createBook.goodreadsId, saved.book.goodreadsId)
        Assertions.assertNull(saved.book.librarythingId)
        Assertions.assertEquals(createUserBookDto.owned, saved.owned)
        Assertions.assertEquals(createUserBookDto.toRead, saved.toRead)
        Assertions.assertEquals(createUserBookDto.personalNotes, saved.personalNotes)
        Assertions.assertNull(saved.percentRead)
        Assertions.assertNotNull(saved.creationDate)
        Assertions.assertNotNull(saved.modificationDate)
        Assertions.assertNotNull(saved.book.creationDate)
        Assertions.assertNotNull(saved.book.modificationDate)
        Assertions.assertEquals(ReadingEventType.FINISHED, saved.lastReadingEvent)
        Assertions.assertNotNull(saved.lastReadingEventDate)
        Assertions.assertEquals(1, readingEventService.findAll(null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)

        val thirtyDaysAfter = nowInstant().plus(30, ChronoUnit.DAYS)
        val newEvent = readingEventService.save(
            CreateReadingEventDto(
                ReadingEventType.CURRENTLY_READING,
                saved.book.id,
                thirtyDaysAfter
            ),
            user()
        )
        Assertions.assertEquals(ReadingEventType.CURRENTLY_READING, newEvent.eventType)

        var userbook = bookService.findUserBookById(saved.id!!)
        Assertions.assertEquals(ReadingEventType.CURRENTLY_READING, userbook.lastReadingEvent)
        Assertions.assertTrue(userbook.lastReadingEventDate?.isAfter(nowInstant().plus(29, ChronoUnit.DAYS))!!)

        Assertions.assertEquals(2, readingEventService.findAll(null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)

        readingEventService.deleteReadingEventById(newEvent.id!!)
        Assertions.assertEquals(1, readingEventService.findAll(null, null, Pageable.ofSize(30)).totalElements)
        userbook = bookService.findUserBookById(saved.id!!)
        Assertions.assertEquals(ReadingEventType.FINISHED, userbook.lastReadingEvent)
        Assertions.assertTrue(userbook.lastReadingEventDate?.isAfter(nowInstant().minus(2, ChronoUnit.MINUTES))!!)
    }

    @Test
    fun newEventWithoutExistingUserbookShouldCreateUserbook() {
        val user = user()
        val createBook = bookDto()
        val savedBook = bookService.save(createBook, null)
        val nbBooks = bookService.findAll(null, null, null, null, null, null, Pageable.ofSize(30), user(), LibraryFilter.ANY).totalElements
        Assertions.assertEquals(1, nbBooks)

        var nbUserBooks = bookService.findUserBookByCriteria(user.id.value, null, null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(0, nbUserBooks)

        Assertions.assertEquals(0, readingEventService.findAll(null, null, Pageable.ofSize(30)).totalElements)

        val dateAfter = nowInstant().plus(1, ChronoUnit.DAYS)
        val newEvent = readingEventService.save(
            CreateReadingEventDto(
                ReadingEventType.CURRENTLY_READING,
                savedBook.id,
                dateAfter
            ),
            user()
        )
        Assertions.assertEquals(ReadingEventType.CURRENTLY_READING, newEvent.eventType)
        nbUserBooks = bookService.findUserBookByCriteria(user.id.value, null, null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(1, nbUserBooks)

        Assertions.assertEquals(1, readingEventService.findAll(null, null, Pageable.ofSize(30)).totalElements)
    }

    @Test
    fun newEventWithoutBookIdShouldThrowException() {
        val user = user()
        val nbBooks = bookService.findAll(null, null, null, null, null, null, Pageable.ofSize(30), user(), LibraryFilter.ANY).totalElements
        Assertions.assertEquals(0, nbBooks)

        var nbUserBooks = bookService.findUserBookByCriteria(user.id.value, null, null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(0, nbUserBooks)

        val dateAfter = nowInstant().plus(1, ChronoUnit.DAYS)
        assertThrows<JeluException>("bookId is required") {
            readingEventService.save(
                CreateReadingEventDto(
                    ReadingEventType.CURRENTLY_READING,
                    null,
                    dateAfter
                ),
                user()
            )
        }
        Assertions.assertEquals(0, readingEventService.findAll(null, null, Pageable.ofSize(30)).totalElements)
    }

    @Test
    fun testUpdateEvent() {
        val createBook = bookDto()
        val createUserBookDto = createUserBookDto(createBook, ReadingEventType.FINISHED, nowInstant())
        val saved: UserBookLightDto = bookService.save(createUserBookDto, user(), null)
        Assertions.assertEquals(createBook.title, saved.book.title)
        Assertions.assertEquals(createBook.isbn10, saved.book.isbn10)
        Assertions.assertEquals(createBook.isbn13?.trim(), saved.book.isbn13)
        Assertions.assertEquals("This is a test summary with a newline", saved.book.summary)
        Assertions.assertEquals(createBook.publisher, saved.book.publisher)
        Assertions.assertEquals(createBook.pageCount, saved.book.pageCount)
        Assertions.assertEquals(createBook.goodreadsId, saved.book.goodreadsId)
        Assertions.assertNull(saved.book.librarythingId)
        Assertions.assertEquals(createUserBookDto.owned, saved.owned)
        Assertions.assertEquals(createUserBookDto.toRead, saved.toRead)
        Assertions.assertEquals(createUserBookDto.personalNotes, saved.personalNotes)
        Assertions.assertNull(saved.percentRead)
        Assertions.assertNotNull(saved.creationDate)
        Assertions.assertNotNull(saved.modificationDate)
        Assertions.assertNotNull(saved.book.creationDate)
        Assertions.assertNotNull(saved.book.modificationDate)
        Assertions.assertEquals(ReadingEventType.FINISHED, saved.lastReadingEvent)
        Assertions.assertNotNull(saved.lastReadingEventDate)
        Assertions.assertEquals(1, readingEventService.findAll(null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)

        val thirtyDaysAfter = nowInstant().plus(30, ChronoUnit.DAYS)
        val newEvent = readingEventService.save(
            CreateReadingEventDto(
                ReadingEventType.CURRENTLY_READING,
                saved.book.id,
                thirtyDaysAfter
            ),
            user()
        )
        Assertions.assertEquals(ReadingEventType.CURRENTLY_READING, newEvent.eventType)

        var userbook = bookService.findUserBookById(saved.id!!)
        Assertions.assertEquals(ReadingEventType.CURRENTLY_READING, userbook.lastReadingEvent)
        Assertions.assertTrue(userbook.lastReadingEventDate?.isAfter(nowInstant().plus(29, ChronoUnit.DAYS))!!)

        Assertions.assertEquals(2, readingEventService.findAll(null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)

        val updated = readingEventService.updateReadingEvent(
            newEvent.id!!,
            UpdateReadingEventDto(
                ReadingEventType.FINISHED,
                nowInstant().plus(40, ChronoUnit.DAYS)
            )
        )
        Assertions.assertEquals(ReadingEventType.FINISHED, updated.eventType)
        Assertions.assertFalse(updated.creationDate?.isAfter(nowInstant().plus(2, ChronoUnit.HOURS))!!)
        Assertions.assertTrue(updated.modificationDate?.isAfter(nowInstant().plus(39, ChronoUnit.DAYS))!!)
        userbook = bookService.findUserBookById(saved.id!!)
        Assertions.assertEquals(ReadingEventType.FINISHED, userbook.lastReadingEvent)
        Assertions.assertTrue(userbook.lastReadingEventDate?.isAfter(nowInstant().plus(39, ChronoUnit.DAYS))!!)
    }

    fun user(): User {
        val userDetail = userService.loadUserByUsername("testuser")
        return (userDetail as JeluUser).user
    }
}
