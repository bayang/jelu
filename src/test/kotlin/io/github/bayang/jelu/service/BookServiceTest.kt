package io.github.bayang.jelu.service

import io.github.bayang.jelu.authorDto
import io.github.bayang.jelu.bookDto
import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.createUserBookDto
import io.github.bayang.jelu.dao.ReadingEventType
import io.github.bayang.jelu.dao.User
import io.github.bayang.jelu.dto.AuthorDto
import io.github.bayang.jelu.dto.AuthorUpdateDto
import io.github.bayang.jelu.dto.BookCreateDto
import io.github.bayang.jelu.dto.BookDto
import io.github.bayang.jelu.dto.CreateUserDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.dto.LibraryFilter
import io.github.bayang.jelu.dto.UserBookLightDto
import io.github.bayang.jelu.dto.UserBookUpdateDto
import io.github.bayang.jelu.tags
import io.github.bayang.jelu.utils.nowInstant
import io.github.bayang.jelu.utils.slugify
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
import org.springframework.mock.web.MockMultipartFile
import java.io.File

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BookServiceTest(
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
        readingEventService.findAll(null, null, null, Pageable.ofSize(30)).content.forEach {
            readingEventService.deleteReadingEventById(it.id!!)
        }
        bookService.findUserBookByCriteria(user().id.value, null, null, Pageable.ofSize(30))
            .forEach { bookService.deleteUserBookById(it.id!!) }
        bookService.findAllAuthors(null, Pageable.ofSize(30)).forEach {
            bookService.deleteAuthorById(it.id!!)
        }
    }

    @Test
    fun testInsertAuthor() {
        val author = authorDto()
        val res = bookService.save(author)
        Assertions.assertNotNull(res)
        Assertions.assertEquals(author.name, res.name)
        val found = bookService.findAuthorsById(res.id!!)
        Assertions.assertEquals(author.name, found.name)
        Assertions.assertEquals(author.biography, found.biography)
        Assertions.assertEquals(author.wikipediaPage, found.wikipediaPage)
        Assertions.assertNotNull(found.creationDate)
        Assertions.assertNotNull(found.modificationDate)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
    }

    @Test
    fun testUpdateAuthorNoImage() {
        val author = authorDto()
        val res = bookService.save(author)
        Assertions.assertNotNull(res)
        Assertions.assertEquals(author.name, res.name)
        val update = AuthorUpdateDto(biography = null, name = null, dateOfBirth = "2000-12-12", dateOfDeath = null, facebookPage = null, goodreadsPage = null, image = null, instagramPage = null, officialPage = null, twitterPage = null, wikipediaPage = null, notes = null, creationDate = null, id = null, modificationDate = null)
        val updated: AuthorDto = bookService.updateAuthor(res.id!!, update)
        Assertions.assertEquals(author.name, updated.name)
        Assertions.assertEquals(author.biography, updated.biography)
        Assertions.assertEquals(author.wikipediaPage, updated.wikipediaPage)
        Assertions.assertEquals(update.dateOfBirth, updated.dateOfBirth)
        Assertions.assertNotNull(updated.creationDate)
        Assertions.assertNotNull(updated.modificationDate)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
    }

    @Test
    fun testUpdateAuthorWithImageAndThenReplaceImage() {
        val author = authorDto()
        val res = bookService.save(author)
        Assertions.assertNotNull(res)
        Assertions.assertEquals(author.name, res.name)
        Assertions.assertNull(res.image)
        val update = AuthorUpdateDto(biography = null, name = null, dateOfBirth = "2000-12-12", dateOfDeath = null, facebookPage = null, goodreadsPage = null, image = null, instagramPage = null, officialPage = null, twitterPage = null, wikipediaPage = null, notes = null, creationDate = null, id = null, modificationDate = null)
        val uploadFile = MockMultipartFile("test-cover.jpg", "test-cover.jpg", "image/jpeg", this::class.java.getResourceAsStream("test-cover.jpg"))
        val updated: AuthorDto = bookService.updateAuthor(res.id!!, update, uploadFile)
        Assertions.assertEquals(author.name, updated.name)
        Assertions.assertEquals(author.biography, updated.biography)
        Assertions.assertEquals(author.wikipediaPage, updated.wikipediaPage)
        Assertions.assertEquals(update.dateOfBirth, updated.dateOfBirth)
        Assertions.assertNotNull(updated.creationDate)
        Assertions.assertNotNull(updated.modificationDate)
        Assertions.assertNotNull(updated.image)
        Assertions.assertTrue(updated.image?.contains(slugify(updated.name), true)!!)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
        // update new image make sure previous is replaced
        val replacementFile = MockMultipartFile("test-replacement-cover.jpg", "test-replacement-cover.jpg", "image/jpeg", this::class.java.getResourceAsStream("test-cover.jpg"))
        val replaced: AuthorDto = bookService.updateAuthor(res.id!!, update, replacementFile)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
        Assertions.assertTrue(replaced.image?.contains(slugify(updated.name), true)!!)
    }

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
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
    }

    @Test
    fun testInsertBooksWithAuthors() {
        val res: BookDto = bookService.save(bookDto(), null)
        Assertions.assertNotNull(res.id)
        val found = bookService.findBookById(res.id!!)
        Assertions.assertEquals(found.id, res.id)
        Assertions.assertEquals(found.authors?.get(0)?.name, res.authors?.get(0)?.name)
        Assertions.assertEquals(found.title, res.title)
        Assertions.assertEquals(found.isbn10, res.isbn10)
        Assertions.assertEquals(found.pageCount, res.pageCount)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
    }

    @Test
    fun testDeletedAuthorShouldBeRemovedFromBook() {
        val res: BookDto = bookService.save(bookDto(), null)
        Assertions.assertNotNull(res.id)
        val res2: BookDto = bookService.save(bookDto("title2"), null)
        Assertions.assertNotNull(res2.id)
        val found = bookService.findBookById(res.id!!)
        Assertions.assertEquals(found.id, res.id)
        Assertions.assertEquals(found.authors?.get(0)?.name, res.authors?.get(0)?.name)
        Assertions.assertEquals(found.title, res.title)
        Assertions.assertEquals(found.isbn10, res.isbn10)
        Assertions.assertEquals(found.pageCount, res.pageCount)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
        bookService.deleteAuthorById(found.authors?.get(0)?.id!!)
        val foundAfterModification = bookService.findBookById(res.id!!)
        Assertions.assertEquals(0, foundAfterModification.authors?.size)
        val foundAfterModification2 = bookService.findBookById(res2.id!!)
        Assertions.assertEquals(0, foundAfterModification2.authors?.size)
    }

    @Test
    fun testDeleteAuthorOnlyFromOneBookStillExistsInDb() {
        val res: BookDto = bookService.save(bookDto(), null)
        Assertions.assertNotNull(res.id)
        val res2: BookDto = bookService.save(bookDto("title2"), null)
        Assertions.assertNotNull(res2.id)
        val found = bookService.findBookById(res.id!!)
        Assertions.assertEquals(found.id, res.id)
        Assertions.assertEquals(found.authors?.get(0)?.name, res.authors?.get(0)?.name)
        Assertions.assertEquals(found.title, res.title)
        Assertions.assertEquals(found.isbn10, res.isbn10)
        Assertions.assertEquals(found.pageCount, res.pageCount)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
        val authorId = res.authors?.get(0)?.id
        val authorName = res.authors?.get(0)?.name
        bookService.deleteAuthorFromBook(res.id!!, authorId!!)
        val foundAfterModification = bookService.findBookById(res.id!!)
        Assertions.assertEquals(0, foundAfterModification.authors?.size)
        val foundAfterModification2 = bookService.findBookById(res2.id!!)
        Assertions.assertEquals(1, foundAfterModification2.authors?.size)
        val authorStillInDb = bookService.findAuthorsById(authorId)
        Assertions.assertEquals(authorName, authorStillInDb.name)
    }

    @Test
    fun testInsertUserbookWithNewBookNoImage() {
        val createBook = bookDto()
        val createUserBookDto = createUserBookDto(createBook)
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
        Assertions.assertNotNull(saved.creationDate)
        Assertions.assertNotNull(saved.modificationDate)
        Assertions.assertNotNull(saved.book.creationDate)
        Assertions.assertNotNull(saved.book.modificationDate)
        Assertions.assertNull(saved.book.image)
        Assertions.assertNull(saved.lastReadingEvent)
        Assertions.assertNull(saved.lastReadingEventDate)
        Assertions.assertTrue(readingEventService.findAll(null, null, null, Pageable.ofSize(30)).isEmpty)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
    }

    @Test
    fun testInsertUserbookWithNewBookImageAndEvent() {
        val createBook = bookDto()
        val createUserBookDto = createUserBookDto(createBook, ReadingEventType.CURRENTLY_READING, nowInstant())
        val uploadFile = MockMultipartFile("test-cover.jpg", "test-cover.jpg", "image/jpeg", this::class.java.getResourceAsStream("test-cover.jpg"))
        val saved: UserBookLightDto = bookService.save(createUserBookDto, user(), uploadFile)
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
        Assertions.assertNotNull(saved.creationDate)
        Assertions.assertNotNull(saved.modificationDate)
        Assertions.assertNotNull(saved.book.creationDate)
        Assertions.assertNotNull(saved.book.modificationDate)
        Assertions.assertTrue(saved.book.image!!.contains(slugify(saved.book.title), true))
        Assertions.assertEquals(ReadingEventType.CURRENTLY_READING, saved.lastReadingEvent)
        Assertions.assertNotNull(saved.lastReadingEventDate)
        Assertions.assertEquals(1, readingEventService.findAll(null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
    }

    @Test
    fun testInsertUserbookWithExistingBookNoImage() {
        val createBook = bookDto()
        val savedBook = bookService.save(createBook, null)
        Assertions.assertEquals(createBook.title, savedBook.title)
        Assertions.assertNotNull(savedBook.id)
        val modified = BookCreateDto(
            savedBook.id,
            title = "modified title",
            isbn10 = savedBook.isbn10,
            isbn13 = savedBook.isbn13
        )
        val createUserBookDto = createUserBookDto(modified)
        val saved: UserBookLightDto = bookService.save(createUserBookDto, user(), null)
        Assertions.assertEquals(modified.title, saved.book.title)
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
        Assertions.assertNotNull(saved.creationDate)
        Assertions.assertNotNull(saved.modificationDate)
        Assertions.assertNotNull(saved.book.creationDate)
        Assertions.assertNotNull(saved.book.modificationDate)
        Assertions.assertNull(saved.lastReadingEvent)
        Assertions.assertNull(saved.lastReadingEventDate)
        Assertions.assertTrue(readingEventService.findAll(null, null, null, Pageable.ofSize(30)).isEmpty)
        Assertions.assertNull(saved.book.image)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
    }

    @Test
    fun testInsertUserbookWithExistingBookAndExistingBookHasImage() {
        val createBook = bookDto()
        val uploadFile = MockMultipartFile("test-cover.jpg", "test-cover.jpg", "image/jpeg", this::class.java.getResourceAsStream("test-cover.jpg"))
        val savedBook = bookService.save(createBook, uploadFile)
        Assertions.assertEquals(createBook.title, savedBook.title)
        Assertions.assertNotNull(savedBook.id)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
        val modified = BookCreateDto(
            savedBook.id,
            title = "modified title",
            isbn10 = savedBook.isbn10,
            isbn13 = savedBook.isbn13
        )
        val createUserBookDto = createUserBookDto(modified)
        val saved: UserBookLightDto = bookService.save(createUserBookDto, user(), null)
        Assertions.assertEquals(modified.title, saved.book.title)
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
        Assertions.assertNotNull(saved.creationDate)
        Assertions.assertNotNull(saved.modificationDate)
        Assertions.assertNotNull(saved.book.creationDate)
        Assertions.assertNotNull(saved.book.modificationDate)
        Assertions.assertNull(saved.lastReadingEvent)
        Assertions.assertNull(saved.lastReadingEventDate)
        Assertions.assertTrue(readingEventService.findAll(null, null, null, Pageable.ofSize(30)).isEmpty)
        Assertions.assertTrue(saved.book.image!!.contains(slugify(savedBook.title), true))
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
    }

    @Test
    fun testInsertUserbookWithImageAndExistingBookAndExistingBookHasImage() {
        val createBook = bookDto()
        val uploadFile = MockMultipartFile("test-cover.jpg", "test-cover.jpg", "image/jpeg", this::class.java.getResourceAsStream("test-cover.jpg"))
        val savedBook = bookService.save(createBook, uploadFile)
        Assertions.assertEquals(createBook.title, savedBook.title)
        Assertions.assertNotNull(savedBook.id)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
        val modified = BookCreateDto(
            savedBook.id,
            title = "modified title",
            isbn10 = savedBook.isbn10,
            isbn13 = savedBook.isbn13
        )
        val createUserBookDto = createUserBookDto(modified)
        val replacementFile = MockMultipartFile("test-replace-cover.jpg", "test-replace-cover.jpg", "image/jpeg", this::class.java.getResourceAsStream("test-cover.jpg"))
        val saved: UserBookLightDto = bookService.save(createUserBookDto, user(), replacementFile)
        Assertions.assertEquals(modified.title, saved.book.title)
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
        Assertions.assertNotNull(saved.creationDate)
        Assertions.assertNotNull(saved.modificationDate)
        Assertions.assertNotNull(saved.book.creationDate)
        Assertions.assertNotNull(saved.book.modificationDate)
        Assertions.assertNull(saved.lastReadingEvent)
        Assertions.assertNull(saved.lastReadingEventDate)
        Assertions.assertTrue(readingEventService.findAll(null, null, null, Pageable.ofSize(30)).isEmpty)
        Assertions.assertTrue(saved.book.image!!.contains(slugify(modified.title), true))
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
    }

    @Test
    fun testUpdateUserbookWithImageAndEventNoNewEvent() {
        val createBook = bookDto()
        val createUserBookDto = createUserBookDto(createBook, ReadingEventType.CURRENTLY_READING, nowInstant())
        val uploadFile = MockMultipartFile("test-cover.jpg", "test-cover.jpg", "image/jpeg", this::class.java.getResourceAsStream("test-cover.jpg"))
        val saved: UserBookLightDto = bookService.save(createUserBookDto, user(), uploadFile)
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
        Assertions.assertTrue(saved.book.image!!.contains(slugify(saved.book.title), true))
        Assertions.assertEquals(ReadingEventType.CURRENTLY_READING, saved.lastReadingEvent)
        Assertions.assertNotNull(saved.lastReadingEventDate)
        Assertions.assertEquals(1, readingEventService.findAll(null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)

        val updater = UserBookUpdateDto(
            ReadingEventType.FINISHED,
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
        Assertions.assertTrue(updated.book.image!!.contains(slugify(updated.book.title), true))
        Assertions.assertEquals(ReadingEventType.FINISHED, updated.lastReadingEvent)
        Assertions.assertNotNull(updated.lastReadingEventDate)
        Assertions.assertEquals(1, updated.readingEvents?.size)
        Assertions.assertEquals(1, readingEventService.findAll(null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
    }

    @Test
    fun testUpdateUserbookWithImageAndEventNewEventRequired() {
        val createBook = bookDto()
        val createUserBookDto = createUserBookDto(createBook, ReadingEventType.FINISHED, nowInstant())
        val uploadFile = MockMultipartFile("test-cover.jpg", "test-cover.jpg", "image/jpeg", this::class.java.getResourceAsStream("test-cover.jpg"))
        val saved: UserBookLightDto = bookService.save(createUserBookDto, user(), uploadFile)
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
        Assertions.assertTrue(saved.book.image!!.contains(slugify(saved.book.title), true))
        Assertions.assertEquals(ReadingEventType.FINISHED, saved.lastReadingEvent)
        Assertions.assertNotNull(saved.lastReadingEventDate)
        Assertions.assertEquals(1, readingEventService.findAll(null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)

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
        Assertions.assertTrue(updated.book.image!!.contains(slugify(updated.book.title), true))
        Assertions.assertEquals(ReadingEventType.DROPPED, updated.lastReadingEvent)
        Assertions.assertNotNull(updated.lastReadingEventDate)
        Assertions.assertEquals(2, updated.readingEvents?.size)
        Assertions.assertEquals(2, readingEventService.findAll(null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
    }

    @Test
    fun testUpdateUserbookWithImageAndEventNewEventRequiredAndNewImage() {
        val createBook = bookDto()
        val createUserBookDto = createUserBookDto(createBook, ReadingEventType.FINISHED, nowInstant())
        val uploadFile = MockMultipartFile("test-cover.jpg", "test-cover.jpg", "image/jpeg", this::class.java.getResourceAsStream("test-cover.jpg"))
        val saved: UserBookLightDto = bookService.save(createUserBookDto, user(), uploadFile)
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
        Assertions.assertTrue(saved.book.image!!.contains(slugify(saved.book.title), true))
        Assertions.assertEquals(ReadingEventType.FINISHED, saved.lastReadingEvent)
        Assertions.assertNotNull(saved.lastReadingEventDate)
        Assertions.assertEquals(1, readingEventService.findAll(null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)

        val updater = UserBookUpdateDto(
            ReadingEventType.DROPPED,
            personalNotes = "new notes",
            owned = false,
            book = null,
            toRead = null,
            percentRead = 50
        )
        val replacementFile = MockMultipartFile("test-replace-cover.jpg", "test-replace-cover.jpg", "image/jpeg", this::class.java.getResourceAsStream("test-cover.jpg"))
        val updated = bookService.update(saved.id!!, updater, replacementFile)
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
        Assertions.assertTrue(updated.book.image!!.contains(slugify(updated.book.title), true))
        Assertions.assertEquals(ReadingEventType.DROPPED, updated.lastReadingEvent)
        Assertions.assertNotNull(updated.lastReadingEventDate)
        Assertions.assertEquals(2, updated.readingEvents?.size)
        Assertions.assertEquals(2, readingEventService.findAll(null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
    }

    @Test
    fun deleteUserBookRemovesEventsDoesNotRemoveBook() {
        val createBook = bookDto()
        val uploadFile = MockMultipartFile("test-cover.jpg", "test-cover.jpg", "image/jpeg", this::class.java.getResourceAsStream("test-cover.jpg"))
        val savedBook = bookService.save(createBook, uploadFile)
        Assertions.assertEquals(createBook.title, savedBook.title)
        Assertions.assertNotNull(savedBook.id)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
        val modified = BookCreateDto(
            savedBook.id,
            title = "modified title",
            isbn10 = savedBook.isbn10,
            isbn13 = savedBook.isbn13
        )
        val createUserBookDto = createUserBookDto(modified, ReadingEventType.FINISHED)
        val saved1: UserBookLightDto = bookService.save(createUserBookDto, user(), null)
        val second = BookCreateDto(id = savedBook.id)
        val createUserBookDto2 = createUserBookDto(second, ReadingEventType.FINISHED)
        val saved2: UserBookLightDto = bookService.save(createUserBookDto2, user(), null)

        Assertions.assertNotNull(saved1)
        Assertions.assertNotNull(saved2)
        var nb = bookService.findUserBookByCriteria(user().id.value, null, null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(2, nb)
        var eventsNb = readingEventService.findAll(null, null, null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(2, eventsNb)
        bookService.deleteUserBookById(saved1.id!!)
        nb = bookService.findUserBookByCriteria(user().id.value, null, null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(1, nb)
        eventsNb = readingEventService.findAll(null, null, null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(1, eventsNb)
    }

    @Test
    fun deleteBookCascades() {
        val createBook = bookDto(withTags = true)
        val uploadFile = MockMultipartFile("test-cover.jpg", "test-cover.jpg", "image/jpeg", this::class.java.getResourceAsStream("test-cover.jpg"))
        val savedBook = bookService.save(createBook, uploadFile)
        Assertions.assertEquals(createBook.title, savedBook.title)
        Assertions.assertNotNull(savedBook.id)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
        val modified = BookCreateDto(
            savedBook.id,
            title = "modified title",
            isbn10 = savedBook.isbn10,
            isbn13 = savedBook.isbn13
        )
        val createUserBookDto = createUserBookDto(modified, ReadingEventType.FINISHED)
        val saved1: UserBookLightDto = bookService.save(createUserBookDto, user(), null)
        val second = BookCreateDto(id = savedBook.id)
        val createUserBookDto2 = createUserBookDto(second, ReadingEventType.FINISHED)
        val saved2: UserBookLightDto = bookService.save(createUserBookDto2, user(), null)

        Assertions.assertNotNull(saved1)
        Assertions.assertNotNull(saved2)
        var nb = bookService.findUserBookByCriteria(user().id.value, null, null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(2, nb)
        var eventsNb = readingEventService.findAll(null, null, null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(2, eventsNb)
        var authorsNb = bookService.findAllAuthors(null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(1, authorsNb)
        var tagsNb = bookService.findAllTags(null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(2, tagsNb)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
        bookService.deleteBookById(savedBook.id!!)
        nb = bookService.findUserBookByCriteria(user().id.value, null, null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(0, nb)
        authorsNb = bookService.findAllAuthors(null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(1, authorsNb)
        eventsNb = readingEventService.findAll(null, null, null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(0, eventsNb)
        tagsNb = bookService.findAllTags(null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(2, tagsNb)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
    }

    @Test
    fun testRemoveTagFromOneBook() {
        val createBook = bookDto(withTags = true)
        val uploadFile = MockMultipartFile("test-cover.jpg", "test-cover.jpg", "image/jpeg", this::class.java.getResourceAsStream("test-cover.jpg"))
        val savedBook = bookService.save(createBook, uploadFile)
        Assertions.assertEquals(createBook.title, savedBook.title)
        Assertions.assertNotNull(savedBook.id)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
        val modified = BookCreateDto(
            savedBook.id,
            title = "modified title",
            isbn10 = savedBook.isbn10,
            isbn13 = savedBook.isbn13
        )
        val createUserBookDto = createUserBookDto(modified, ReadingEventType.FINISHED)
        val saved1: UserBookLightDto = bookService.save(createUserBookDto, user(), null)
        val createBook2 = bookDto(withTags = true)
        val savedBook2 = bookService.save(createBook2, uploadFile)
        Assertions.assertEquals(createBook.title, savedBook.title)
        Assertions.assertEquals(2, savedBook2.tags?.size)
        Assertions.assertEquals(2, saved1.book.tags?.size)
        var tagsNb = bookService.findAllTags(null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(2, tagsNb)
        val tagId = bookService.findAllTags("fantasy", Pageable.ofSize(30)).content[0].id
        bookService.deleteTagFromBook(savedBook.id!!, tagId!!)

        val retrieved1 = bookService.findUserBookById(saved1.id!!)
        val retrieved2 = bookService.findBookById(savedBook2.id!!)
        Assertions.assertEquals(2, retrieved2.tags?.size)
        Assertions.assertEquals(1, retrieved1.book.tags?.size)
        tagsNb = bookService.findAllTags(null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(2, tagsNb)
    }

    @Test
    fun testRemoveTagFromDb() {
        val createBook = bookDto(withTags = true)
        val uploadFile = MockMultipartFile("test-cover.jpg", "test-cover.jpg", "image/jpeg", this::class.java.getResourceAsStream("test-cover.jpg"))
        val savedBook = bookService.save(createBook, uploadFile)
        Assertions.assertEquals(createBook.title, savedBook.title)
        Assertions.assertNotNull(savedBook.id)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
        val modified = BookCreateDto(
            savedBook.id,
            title = "modified title",
            isbn10 = savedBook.isbn10,
            isbn13 = savedBook.isbn13
        )
        val createUserBookDto = createUserBookDto(modified, ReadingEventType.FINISHED)
        val saved1: UserBookLightDto = bookService.save(createUserBookDto, user(), null)
        val createBook2 = bookDto(withTags = true)
        val savedBook2 = bookService.save(createBook2, uploadFile)
        Assertions.assertEquals(createBook.title, savedBook.title)
        Assertions.assertEquals(2, savedBook2.tags?.size)
        Assertions.assertEquals(2, saved1.book.tags?.size)
        var tagsNb = bookService.findAllTags(null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(2, tagsNb)
        val tagId = bookService.findAllTags("fantasy", Pageable.ofSize(30)).content[0].id
        bookService.deleteTagById(tagId!!)

        val retrieved1 = bookService.findUserBookById(saved1.id!!)
        val retrieved2 = bookService.findBookById(savedBook2.id!!)
        Assertions.assertEquals(1, retrieved2.tags?.size)
        Assertions.assertEquals(1, retrieved1.book.tags?.size)
        tagsNb = bookService.findAllTags(null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(1, tagsNb)
    }

    @Test
    fun testMergeAuthors() {
        val authorDto1 = authorDto(name = "author1")
        val authorDto2 = authorDto(name = "author2")
        val book1 = BookCreateDto(
            id = null,
            title = "book 1",
            isbn10 = "1566199093",
            isbn13 = "9781566199094 ",
            summary = "This is a test summary\nwith a newline",
            image = "",
            publisher = "test-publisher",
            pageCount = 50,
            publishedDate = "",
            series = "",
            authors = mutableListOf(authorDto1),
            numberInSeries = null,
            tags = tags(),
            goodreadsId = "4321abc",
            googleId = "1234",
            librarythingId = "",
            language = "",
            amazonId = ""
        )
        val book2 = BookCreateDto(
            id = null,
            title = "book 2",
            isbn10 = "1566199093",
            isbn13 = "9781566199094 ",
            summary = "This is a test summary\nwith a newline",
            image = "",
            publisher = "test-publisher",
            pageCount = 50,
            publishedDate = "",
            series = "",
            authors = mutableListOf(authorDto2),
            numberInSeries = null,
            tags = tags(),
            goodreadsId = "4321abc",
            googleId = "1234",
            librarythingId = "",
            language = "",
            amazonId = ""
        )
        val book3 = BookCreateDto(
            id = null,
            title = "book 3",
            isbn10 = "1566199093",
            isbn13 = "9781566199094 ",
            summary = "This is a test summary\nwith a newline",
            image = "",
            publisher = "test-publisher",
            pageCount = 50,
            publishedDate = "",
            series = "",
            authors = mutableListOf(authorDto2, authorDto1),
            numberInSeries = null,
            tags = tags(),
            goodreadsId = "4321abc",
            googleId = "1234",
            librarythingId = "",
            language = "",
            amazonId = ""
        )
        val savedBook1 = bookService.save(book1, null)
        Assertions.assertNotNull(savedBook1)
        Assertions.assertEquals(authorDto1.name, savedBook1.authors?.get(0)?.name)
        val savedBook2 = bookService.save(book2, null)
        Assertions.assertNotNull(savedBook2)
        Assertions.assertEquals(authorDto2.name, savedBook2.authors?.get(0)?.name)
        val savedBook3 = bookService.save(book3, null)
        Assertions.assertNotNull(savedBook3)
        Assertions.assertEquals(2, savedBook3.authors?.size)
        val authorId1 = savedBook1.authors?.get(0)?.id
        val authorId2 = savedBook2.authors?.get(0)?.id
        val author1BooksNb = bookService.findAuthorBooksById(authorId1!!, user(), Pageable.ofSize(30), LibraryFilter.ANY).totalElements
        val author2BooksNb = bookService.findAuthorBooksById(authorId2!!, user(), Pageable.ofSize(30), LibraryFilter.ANY).totalElements
        Assertions.assertEquals(2, author1BooksNb)
        Assertions.assertEquals(2, author2BooksNb)
        var authorsNb = bookService.findAllAuthors(null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(2, authorsNb)
        val update = AuthorUpdateDto(biography = null, name = "author 3", dateOfBirth = "2000-12-12", dateOfDeath = null, facebookPage = null, goodreadsPage = null, image = null, instagramPage = null, officialPage = null, twitterPage = null, wikipediaPage = null, notes = null, creationDate = null, id = null, modificationDate = null)
        val merged = bookService.mergeAuthors(authorId1, authorId2, update, user())
        Assertions.assertEquals(update.name, merged.name)
        val mergedBooks = bookService.findAuthorBooksById(merged.id!!, user(), Pageable.ofSize(30), LibraryFilter.ANY)
        val titles = listOf(book1.title, book2.title, book3.title)
        mergedBooks.forEach {
            Assertions.assertTrue(titles.contains(it.title))
        }
        Assertions.assertEquals(3, mergedBooks.totalElements)
        authorsNb = bookService.findAllAuthors(null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(1, authorsNb)
    }

    fun user(): User {
        val userDetail = userService.loadUserByUsername("testuser")
        return (userDetail as JeluUser).user
    }
}
