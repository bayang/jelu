package io.github.bayang.jelu.service.exports

import io.github.bayang.jelu.authorDto
import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.createUserBookDto
import io.github.bayang.jelu.dao.ReadingEventType
import io.github.bayang.jelu.dao.User
import io.github.bayang.jelu.dto.BookCreateDto
import io.github.bayang.jelu.dto.CreateReadingEventDto
import io.github.bayang.jelu.dto.CreateUserBookDto
import io.github.bayang.jelu.dto.CreateUserDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.dto.UserBookLightDto
import io.github.bayang.jelu.service.BookService
import io.github.bayang.jelu.service.ReadingEventService
import io.github.bayang.jelu.service.UserMessageService
import io.github.bayang.jelu.service.UserService
import io.github.bayang.jelu.tags
import org.assertj.core.util.Files
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Pageable
import java.io.File
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Locale

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CsvExportServiceTest(
    @Autowired private val csvExportService: CsvExportService,
    @Autowired private val bookService: BookService,
    @Autowired private val userService: UserService,
    @Autowired private val jeluProperties: JeluProperties,
    @Autowired private val readingEventService: ReadingEventService,
    @Autowired private val userMessageService: UserMessageService,
) {

    companion object {
        @TempDir
        lateinit var tempDir: File
    }

    @BeforeAll
    fun setupUser() {
        userService.save(CreateUserDto(login = "testuser", password = "1234", isAdmin = true))
        jeluProperties.files.imports = tempDir.absolutePath
        println(jeluProperties.files.imports)
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
        readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).content.forEach {
            readingEventService.deleteReadingEventById(it.id!!)
        }
        bookService.findUserBookByCriteria(user().id.value, null, null, null, null, null, Pageable.ofSize(30))
            .forEach { bookService.deleteUserBookById(it.id!!) }
        bookService.findAllAuthors(null, Pageable.ofSize(30)).forEach {
            bookService.deleteAuthorById(it.id!!)
        }
        userMessageService.find(user(), null, null, Pageable.ofSize(200))
            .forEach { userMessageDto -> userMessageService.delete(userMessageDto.id!!) }
    }

    @Test
    fun testExport() {
        val book1 = BookCreateDto(
            id = null,
            title = "book1",
            isbn10 = "1566199093",
            isbn13 = "9781566199094 ",
            summary = "This is a test summary\nwith a newline",
            image = "",
            publisher = "test-publisher",
            pageCount = 50,
            publishedDate = "",
            authors = mutableListOf(authorDto(), authorDto("author2 name")),
            tags = tags(),
            goodreadsId = "4321abc",
            googleId = "1234",
            librarythingId = "",
            language = "",
            amazonId = "",
        )
        val createUserBookDto1 = CreateUserBookDto(
            personalNotes = "test personal notes\nwith a newline",
            lastReadingEvent = null,
            lastReadingEventDate = null,
            owned = false,
            toRead = true,
            percentRead = null,
            book = book1,
            borrowed = null,
            currentPageNumber = null,
        )
        val saved1: UserBookLightDto = bookService.save(createUserBookDto1, user(), null)

        val book2 = BookCreateDto(
            id = null,
            title = "book2",
            isbn10 = "1566199093",
            isbn13 = "9781566199094 ",
            summary = "This is a test summary\nwith a newline",
            image = "",
            publisher = "test-publisher",
            pageCount = 50,
            publishedDate = "",
            authors = mutableListOf(authorDto()),
            tags = emptyList(),
            goodreadsId = "4321abc",
            googleId = "1234",
            librarythingId = "",
            language = "",
            amazonId = "",
        )
        val offset = OffsetDateTime.now(ZoneId.systemDefault()).offset
        val date1 = OffsetDateTime.of(2022, 2, 10, 6, 30, 0, 0, offset)
        val createUserBookDto2 = createUserBookDto(book2, ReadingEventType.FINISHED, date1.toInstant())
        val saved2: UserBookLightDto = bookService.save(createUserBookDto2, user(), null)
        val date3 = OffsetDateTime.of(2020, 2, 10, 6, 30, 0, 0, offset)
        val date2 = OffsetDateTime.of(2021, 2, 10, 6, 30, 0, 0, offset)
        readingEventService.save(
            CreateReadingEventDto(
                ReadingEventType.FINISHED,
                saved2.book.id,
                date3.toInstant(),
                null,
            ),
            user(),
        )
        readingEventService.save(
            CreateReadingEventDto(
                ReadingEventType.FINISHED,
                saved2.book.id,
                date2.toInstant(),
                null,
            ),
            user(),
        )

        readingEventService.save(
            CreateReadingEventDto(
                ReadingEventType.DROPPED,
                saved1.book.id,
                date3.toInstant(),
                null,
            ),
            user(),
        )
        readingEventService.save(
            CreateReadingEventDto(
                ReadingEventType.DROPPED,
                saved1.book.id,
                date2.toInstant(),
                null,
            ),
            user(),
        )
        readingEventService.save(
            CreateReadingEventDto(
                ReadingEventType.CURRENTLY_READING,
                saved1.book.id,
                date1.toInstant(),
                null,
            ),
            user(),
        )
        csvExportService.export(user(), Locale.ENGLISH)
        val csv = File(jeluProperties.files.imports).listFiles()[0]
        var content = Files.linesOf(csv, Charsets.UTF_8)
        val expectedCsv = File(this::class.java.getResource("/csv-export/expected.csv").file)
        val linesOf = Files.linesOf(expectedCsv, Charsets.UTF_8)
        Assertions.assertEquals(linesOf.size, content.size)
        Assertions.assertTrue(linesOf.containsAll(content))
        val fileBeginning = "jelu-export-${user().login}"
        Assertions.assertTrue(csv.name.startsWith(fileBeginning, true))
        val messages = userMessageService.find(user(), false, null, Pageable.ofSize(30))
        Assertions.assertEquals(2, messages.numberOfElements)
    }

    fun user(): User {
        val userDetail = userService.loadUserByUsername("testuser")
        return (userDetail as JeluUser).user
    }
}
