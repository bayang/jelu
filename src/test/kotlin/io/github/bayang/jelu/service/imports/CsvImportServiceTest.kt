package io.github.bayang.jelu.service.imports

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dao.ProcessingStatus
import io.github.bayang.jelu.dao.User
import io.github.bayang.jelu.dto.CreateUserDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.dto.UserBookWithoutEventsAndUserDto
import io.github.bayang.jelu.importConfigurationDto
import io.github.bayang.jelu.service.BookService
import io.github.bayang.jelu.service.ImportService
import io.github.bayang.jelu.service.ReadingEventService
import io.github.bayang.jelu.service.UserService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.io.File

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CsvImportServiceTest(
    @Autowired private val bookService: BookService,
    @Autowired private val userService: UserService,
    @Autowired private val jeluProperties: JeluProperties,
    @Autowired private val readingEventService: ReadingEventService,
    @Autowired private val csvImportService: CsvImportService,
    @Autowired private val importService: ImportService
) {

    companion object {
        @TempDir
        lateinit var tempDir: File
    }

    @BeforeAll
    fun setupUser() {
        userService.save(CreateUserDto(login = "testuser", password = "1234", isAdmin = true))
        jeluProperties.files.imports = tempDir.absolutePath
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
        importService.deleteByprocessingStatusAndUser(ProcessingStatus.SAVED, user().id.value)
        readingEventService.findAll(null, null, Pageable.ofSize(30)).content.forEach {
            readingEventService.deleteReadingEventById(it.id!!)
        }
        bookService.findUserBookByCriteria(user().id.value, null, null, Pageable.ofSize(30))
            .forEach { bookService.deleteUserBookById(it.id!!) }
        bookService.findAllAuthors(null, Pageable.ofSize(30)).forEach {
            bookService.deleteAuthorById(it.id!!)
        }
    }

    @Test
    fun testParse() {
        val userId = user().id.value
        val csv = File(this::class.java.getResource("/csv-import/goodreads1.csv").file)
        csvImportService.parse(csv, userId, importConfigurationDto())
        val nb = importService.countByprocessingStatusAndUser(ProcessingStatus.SAVED, userId)
        Assertions.assertEquals(10, nb)
        val dtos = importService.getByprocessingStatusAndUser(ProcessingStatus.SAVED, userId)
        dtos.forEach {
            if (it.title.equals("La somme de nos folies")) {
                Assertions.assertEquals("9782843048302", it.isbn13)
                Assertions.assertEquals("Éditions Zulma", it.publisher)
                val shelves = it.tags?.split(",")
                Assertions.assertEquals(3, shelves?.size)
            }
        }
    }

    @Test
    fun testNoDuplicates() {
        val userId = user().id.value
        val csv = File(this::class.java.getResource("/csv-import/goodreads-duplicate-events.csv").file)
        csvImportService.parse(csv, userId, importConfigurationDto())
        val nb = importService.countByprocessingStatusAndUser(ProcessingStatus.SAVED, userId)
        Assertions.assertEquals(5, nb)
        val dtos = importService.getByprocessingStatusAndUser(ProcessingStatus.SAVED, userId)

        val (success, failures) = csvImportService.importFromDb(userId, importConfigurationDto())
        Assertions.assertEquals(5, success)
        Assertions.assertEquals(0, failures)
        val userbooksPage: Page<UserBookWithoutEventsAndUserDto> = bookService.findUserBookByCriteria(userId, null, null, Pageable.ofSize(30))
        userbooksPage.content.forEach {
            val userbook = bookService.findUserBookById(it.id!!)
            if (userbook.book.title == "The Gulag Archipelago 1918–1956 (Abridged)") {
                Assertions.assertEquals(2, userbook.readingEvents?.size)
            } else {
                Assertions.assertEquals(1, userbook.readingEvents?.size)
            }
        }
    }

    fun user(): User {
        val userDetail = userService.loadUserByUsername("testuser")
        return (userDetail as JeluUser).user
    }
}
