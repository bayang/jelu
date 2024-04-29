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
import io.github.bayang.jelu.dto.SeriesOrderDto
import io.github.bayang.jelu.dto.SeriesUpdateDto
import io.github.bayang.jelu.dto.UserBookLightDto
import io.github.bayang.jelu.dto.UserBookUpdateDto
import io.github.bayang.jelu.dto.fromBookCreateDto
import io.github.bayang.jelu.search.LuceneEntity
import io.github.bayang.jelu.search.LuceneHelper
import io.github.bayang.jelu.tagDto
import io.github.bayang.jelu.tags
import io.github.bayang.jelu.utils.nowInstant
import io.github.bayang.jelu.utils.slugify
import org.apache.lucene.index.Term
import org.jetbrains.exposed.dao.exceptions.EntityNotFoundException
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.mock.web.MockMultipartFile
import java.io.File
import java.util.UUID

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BookServiceTest(
    @Autowired private val bookService: BookService,
    @Autowired private val userService: UserService,
    @Autowired private val jeluProperties: JeluProperties,
    @Autowired private val readingEventService: ReadingEventService,
    @Autowired private val luceneHelper: LuceneHelper,
) {

    companion object {
        @TempDir
        lateinit var tempDir: File
    }

    @BeforeAll
    fun setupUser() {
        userService.save(CreateUserDto(login = "testuser", password = "1234", isAdmin = true))
        jeluProperties.files.images = tempDir.absolutePath
        luceneHelper.getIndexWriter().use { indexWriter ->
            indexWriter.deleteDocuments(Term(LuceneEntity.TYPE, LuceneEntity.Book.type))
            indexWriter.deleteDocuments(Term(LuceneEntity.TYPE, LuceneEntity.Author.type))
        }
    }

    @AfterAll
    fun teardDown() {
        userService.findAll(null).forEach { userService.deleteUser(it.id!!) }
    }

    @AfterEach
    fun cleanTest() {
        tempDir.listFiles()?.forEach {
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
        bookService.findAllTags(null, Pageable.ofSize(20)).content.forEach {
            bookService.deleteTagById(it.id!!)
        }
        bookService.findAllSeries(null, Pageable.ofSize(20)).content.forEach {
            bookService.deleteSeriesById(it.id!!)
        }
        luceneHelper.getIndexWriter().use { indexWriter ->
            indexWriter.deleteDocuments(Term(LuceneEntity.TYPE, LuceneEntity.Book.type))
            indexWriter.deleteDocuments(Term(LuceneEntity.TYPE, LuceneEntity.Author.type))
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
    fun testInsertBooksSeries() {
        val s1 = SeriesOrderDto(name = "series 1", numberInSeries = 1.0, seriesId = null)
        val s2 = SeriesOrderDto(name = "series2", numberInSeries = 1.0, seriesId = null)
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
                // seriesBak = "",
                authors = emptyList(),
                // numberInSeries = null,
                tags = emptyList(),
                goodreadsId = "",
                googleId = "",
                librarythingId = "",
                language = "",
                amazonId = "",
                series = listOf(s1, s2),
            ),
            null,
        )
        Assertions.assertNotNull(res.id)
        val found = bookService.findBookById(res.id!!)
        Assertions.assertEquals(found.id, res.id)
        Assertions.assertEquals(found.authors, res.authors)
        Assertions.assertEquals(found.title, res.title)
        Assertions.assertEquals(found.isbn10, res.isbn10)
        Assertions.assertEquals(found.pageCount, res.pageCount)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
        Assertions.assertEquals(2, res.series?.size)
        val names = listOf(s1.name, s2.name)
        res.series?.forEach {
            Assertions.assertEquals(1.0, it.numberInSeries)
            Assertions.assertTrue(names.contains(it.name))
        }
        var entitiesIds = luceneHelper.searchEntitiesIds("tit", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:ser", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:series", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:(series 1)", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
    }

    @Test
    fun testInsertBooksSeriesNoPosition() {
        val s1 = SeriesOrderDto(name = "series 1", numberInSeries = null)
        val s2 = SeriesOrderDto(name = "series2", numberInSeries = 1.0)
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
                // seriesBak = "",
                authors = emptyList(),
                // numberInSeries = null,
                tags = emptyList(),
                goodreadsId = "",
                googleId = "",
                librarythingId = "",
                language = "",
                amazonId = "",
                series = listOf(s1, s2),
            ),
            null,
        )
        Assertions.assertNotNull(res.id)
        val found = bookService.findBookById(res.id!!)
        Assertions.assertEquals(found.id, res.id)
        Assertions.assertEquals(found.authors, res.authors)
        Assertions.assertEquals(found.title, res.title)
        Assertions.assertEquals(found.isbn10, res.isbn10)
        Assertions.assertEquals(found.pageCount, res.pageCount)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
        Assertions.assertEquals(2, res.series?.size)
        val names = listOf(s1.name, s2.name)
        res.series?.forEach {
            if (it.name == "series 1") {
                Assertions.assertNull(it.numberInSeries)
            } else {
                Assertions.assertEquals(1.0, it.numberInSeries)
            }
            Assertions.assertTrue(names.contains(it.name))
        }
        var entitiesIds = luceneHelper.searchEntitiesIds("tit", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:ser", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:series", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:(series 1)", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
    }

    @Test
    fun testInsertBooksSeriesBothNoPosition() {
        val s1 = SeriesOrderDto(name = "series 1", numberInSeries = null)
        val s2 = SeriesOrderDto(name = "series2", numberInSeries = null)
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
                // seriesBak = "",
                authors = emptyList(),
                // numberInSeries = null,
                tags = emptyList(),
                goodreadsId = "",
                googleId = "",
                librarythingId = "",
                language = "",
                amazonId = "",
                series = listOf(s1, s2),
            ),
            null,
        )
        Assertions.assertNotNull(res.id)
        val found = bookService.findBookById(res.id!!)
        Assertions.assertEquals(found.id, res.id)
        Assertions.assertEquals(found.authors, res.authors)
        Assertions.assertEquals(found.title, res.title)
        Assertions.assertEquals(found.isbn10, res.isbn10)
        Assertions.assertEquals(found.pageCount, res.pageCount)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
        Assertions.assertEquals(2, res.series?.size)
        val names = listOf(s1.name, s2.name)
        res.series?.forEach {
            Assertions.assertNull(it.numberInSeries)
            Assertions.assertTrue(names.contains(it.name))
        }
        var entitiesIds = luceneHelper.searchEntitiesIds("tit", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:ser", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:series", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:(series 1)", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
    }

    @Test
    fun testInsertBooksSeriesBothNoPositionDuplicateSeries() {
        val s1 = SeriesOrderDto(name = "series 1", numberInSeries = null)
        val s2 = SeriesOrderDto(name = "series 1", numberInSeries = null)
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
                // seriesBak = "",
                authors = emptyList(),
                // numberInSeries = null,
                tags = emptyList(),
                goodreadsId = "",
                googleId = "",
                librarythingId = "",
                language = "",
                amazonId = "",
                series = listOf(s1, s2),
            ),
            null,
        )
        Assertions.assertNotNull(res.id)
        val found = bookService.findBookById(res.id!!)
        Assertions.assertEquals(found.id, res.id)
        Assertions.assertEquals(found.authors, res.authors)
        Assertions.assertEquals(found.title, res.title)
        Assertions.assertEquals(found.isbn10, res.isbn10)
        Assertions.assertEquals(found.pageCount, res.pageCount)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
        Assertions.assertEquals(1, res.series?.size)
        Assertions.assertNull(res.series?.get(0)?.numberInSeries)
        Assertions.assertEquals("series 1", res.series?.get(0)?.name)
        var entitiesIds = luceneHelper.searchEntitiesIds("tit", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:ser", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:series", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:(series 1)", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
    }

    @Test
    fun testInsertBooksSeriesBothNoPositionDuplicateExistingSeries() {
        val saved = bookService.saveSeries(SeriesUpdateDto("series 1"))
        Assertions.assertEquals("series 1", saved.name)
        Assertions.assertNotNull(saved.id)
        val s1 = SeriesOrderDto(name = "series 1", numberInSeries = null)
        val s2 = SeriesOrderDto(name = "series 1", numberInSeries = null)
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
                // seriesBak = "",
                authors = emptyList(),
                // numberInSeries = null,
                tags = emptyList(),
                goodreadsId = "",
                googleId = "",
                librarythingId = "",
                language = "",
                amazonId = "",
                series = listOf(s1, s2),
            ),
            null,
        )
        Assertions.assertNotNull(res.id)
        val found = bookService.findBookById(res.id!!)
        Assertions.assertEquals(found.id, res.id)
        Assertions.assertEquals(found.authors, res.authors)
        Assertions.assertEquals(found.title, res.title)
        Assertions.assertEquals(found.isbn10, res.isbn10)
        Assertions.assertEquals(found.pageCount, res.pageCount)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
        Assertions.assertEquals(1, res.series?.size)
        Assertions.assertNull(res.series?.get(0)?.numberInSeries)
        Assertions.assertEquals("series 1", res.series?.get(0)?.name)
        var entitiesIds = luceneHelper.searchEntitiesIds("tit", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:ser", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:series", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:(series 1)", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
    }

    @Test
    fun testInsertBooksSeriesBothNoPositionExistingSeries() {
        val saved = bookService.saveSeries(SeriesUpdateDto("series 1"))
        Assertions.assertEquals("series 1", saved.name)
        Assertions.assertNotNull(saved.id)
        val s1 = SeriesOrderDto(name = "series 1", numberInSeries = null)
        val s2 = SeriesOrderDto(name = "series2", numberInSeries = null)
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
                // seriesBak = "",
                authors = emptyList(),
                // numberInSeries = null,
                tags = emptyList(),
                goodreadsId = "",
                googleId = "",
                librarythingId = "",
                language = "",
                amazonId = "",
                series = listOf(s1, s2),
            ),
            null,
        )
        Assertions.assertNotNull(res.id)
        val found = bookService.findBookById(res.id!!)
        Assertions.assertEquals(found.id, res.id)
        Assertions.assertEquals(found.authors, res.authors)
        Assertions.assertEquals(found.title, res.title)
        Assertions.assertEquals(found.isbn10, res.isbn10)
        Assertions.assertEquals(found.pageCount, res.pageCount)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
        Assertions.assertEquals(2, res.series?.size)
        val names = listOf(s1.name, s2.name)
        res.series?.forEach {
            Assertions.assertNull(it.numberInSeries)
            Assertions.assertTrue(names.contains(it.name))
        }
        var entitiesIds = luceneHelper.searchEntitiesIds("tit", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:ser", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:series", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:(series 1)", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
    }

    @Test
    fun testInsertBooksExistingSeries() {
        val saved = bookService.saveSeries(SeriesUpdateDto("series 1"))
        Assertions.assertEquals("series 1", saved.name)
        Assertions.assertNotNull(saved.id)
        val s1 = SeriesOrderDto(name = "series 1", numberInSeries = 1.0)
        val s2 = SeriesOrderDto(name = "series2", numberInSeries = 1.0)
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
                // seriesBak = "",
                authors = emptyList(),
                // numberInSeries = null,
                tags = emptyList(),
                goodreadsId = "",
                googleId = "",
                librarythingId = "",
                language = "",
                amazonId = "",
                series = listOf(s1, s2),
            ),
            null,
        )
        Assertions.assertNotNull(res.id)
        val found = bookService.findBookById(res.id!!)
        Assertions.assertEquals(found.id, res.id)
        Assertions.assertEquals(found.authors, res.authors)
        Assertions.assertEquals(found.title, res.title)
        Assertions.assertEquals(found.isbn10, res.isbn10)
        Assertions.assertEquals(found.pageCount, res.pageCount)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
        Assertions.assertEquals(2, res.series?.size)
        val names = listOf(s1.name, s2.name)
        res.series?.forEach {
            Assertions.assertEquals(1.0, it.numberInSeries)
            Assertions.assertTrue(names.contains(it.name))
        }
        var entitiesIds = luceneHelper.searchEntitiesIds("tit", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:ser", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
    }

    @Test
    fun testUpdateBooksSeries() {
        val saved = bookService.saveSeries(SeriesUpdateDto("series 1"))
        Assertions.assertEquals("series 1", saved.name)
        Assertions.assertNotNull(saved.id)
        val s1 = SeriesOrderDto(name = "series 1", numberInSeries = 1.0)
        val s2 = SeriesOrderDto(name = "series2", numberInSeries = 1.0)
        val bookCreateDto = BookCreateDto(
            id = null,
            title = "title1",
            isbn10 = "",
            isbn13 = "",
            summary = "",
            image = "",
            publisher = "",
            pageCount = 50,
            publishedDate = "",
            // seriesBak = "",
            authors = emptyList(),
            // numberInSeries = null,
            tags = emptyList(),
            goodreadsId = "",
            googleId = "",
            librarythingId = "",
            language = "",
            amazonId = "",
            series = listOf(s1, s2),
        )
        var res: BookDto = bookService.save(
            bookCreateDto,
            null,
        )
        Assertions.assertNotNull(res.id)
        val found = bookService.findBookById(res.id!!)
        Assertions.assertEquals(found.id, res.id)
        Assertions.assertEquals(found.authors, res.authors)
        Assertions.assertEquals(found.title, res.title)
        Assertions.assertEquals(found.isbn10, res.isbn10)
        Assertions.assertEquals(found.pageCount, res.pageCount)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
        Assertions.assertEquals(2, res.series?.size)
        var names = listOf(s1.name, s2.name)
        res.series?.forEach {
            Assertions.assertEquals(1.0, it.numberInSeries)
            Assertions.assertTrue(names.contains(it.name))
        }
        val series2Id = res.series?.get(1)?.seriesId
        var entitiesIds = luceneHelper.searchEntitiesIds("tit", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:ser", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        var updateDto = fromBookCreateDto(bookCreateDto)
        val list = listOf(res.series?.get(0)!!, SeriesOrderDto(name = "series 3", numberInSeries = 1.0))
        updateDto.series = list
        // change series 1 position, remove series 2 and add a new third series
        updateDto.series?.get(0)?.numberInSeries = 2.2
        res = bookService.update(res.id!!, updateDto)
        Assertions.assertEquals(2, res.series?.size)
        Assertions.assertEquals("series 1", res.series?.get(0)?.name)
        Assertions.assertEquals(2.2, res.series?.get(0)?.numberInSeries)

        Assertions.assertEquals("series 3", res.series?.get(1)?.name)
        Assertions.assertEquals(1.0, res.series?.get(1)?.numberInSeries)

        // series still exists, without books
        val series = bookService.findAllSeries(name = "series2", Pageable.ofSize(5))
        Assertions.assertEquals("series2", series.content[0].name)

        val booksById1 =
            bookService.findSeriesBooksById(saved.id!!, user(), Pageable.ofSize(10), LibraryFilter.ANY)
        Assertions.assertEquals(1, booksById1.totalElements)

        val booksById2 =
            bookService.findSeriesBooksById(series2Id!!, user(), Pageable.ofSize(10), LibraryFilter.ANY)
        Assertions.assertEquals(0, booksById2.totalElements)
    }

    @Test
    fun testInsertBooksExistingSeriesDifferentPosition() {
        val saved = bookService.saveSeries(SeriesUpdateDto("series 1"))
        Assertions.assertEquals("series 1", saved.name)
        Assertions.assertNotNull(saved.id)
        val s1 = SeriesOrderDto(name = "series 1", numberInSeries = 1.0)
        val s2 = SeriesOrderDto(name = "series 1", numberInSeries = 1.5)
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
                // seriesBak = "",
                authors = emptyList(),
                // numberInSeries = null,
                tags = emptyList(),
                goodreadsId = "",
                googleId = "",
                librarythingId = "",
                language = "",
                amazonId = "",
                series = listOf(s1, s2),
            ),
            null,
        )
        Assertions.assertNotNull(res.id)
        val found = bookService.findBookById(res.id!!)
        Assertions.assertEquals(found.id, res.id)
        Assertions.assertEquals(found.authors, res.authors)
        Assertions.assertEquals(found.title, res.title)
        Assertions.assertEquals(found.isbn10, res.isbn10)
        Assertions.assertEquals(found.pageCount, res.pageCount)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
        Assertions.assertEquals(2, res.series?.size)
        val positions = listOf(s1.numberInSeries, s2.numberInSeries)
        res.series?.forEach {
            Assertions.assertEquals("series 1", it.name)
            Assertions.assertTrue(positions.contains(it.numberInSeries))
        }
        var entitiesIds = luceneHelper.searchEntitiesIds("tit", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:ser", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
    }

    @Test
    fun testInsertBooksExistingDuplicateSeries() {
        val saved = bookService.saveSeries(SeriesUpdateDto("series 1"))
        Assertions.assertEquals("series 1", saved.name)
        Assertions.assertNotNull(saved.id)
        val s1 = SeriesOrderDto(name = "series 1", numberInSeries = 1.0)
        val s2 = SeriesOrderDto(name = "series 1", numberInSeries = 1.0)
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
                // seriesBak = "",
                authors = emptyList(),
                // numberInSeries = null,
                tags = emptyList(),
                goodreadsId = "",
                googleId = "",
                librarythingId = "",
                language = "",
                amazonId = "",
                series = listOf(s1, s2),
            ),
            null,
        )
        Assertions.assertNotNull(res.id)
        val found = bookService.findBookById(res.id!!)
        Assertions.assertEquals(found.id, res.id)
        Assertions.assertEquals(found.authors, res.authors)
        Assertions.assertEquals(found.title, res.title)
        Assertions.assertEquals(found.isbn10, res.isbn10)
        Assertions.assertEquals(found.pageCount, res.pageCount)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
        Assertions.assertEquals(1, res.series?.size)
        Assertions.assertEquals(1.0, res.series?.get(0)?.numberInSeries)
        Assertions.assertEquals("series 1", res.series?.get(0)?.name)
        var entitiesIds = luceneHelper.searchEntitiesIds("tit", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:ser", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
    }

    @Test
    fun testDeleteSeriesFromBook() {
        val s1 = SeriesOrderDto(name = "series 1", numberInSeries = 1.0, seriesId = null)
        val s2 = SeriesOrderDto(name = "series2", numberInSeries = 1.0, seriesId = null)
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
                // seriesBak = "",
                authors = emptyList(),
                // numberInSeries = null,
                tags = emptyList(),
                goodreadsId = "",
                googleId = "",
                librarythingId = "",
                language = "",
                amazonId = "",
                series = listOf(s1, s2),
            ),
            null,
        )
        Assertions.assertNotNull(res.id)
        val found = bookService.findBookById(res.id!!)
        Assertions.assertEquals(found.id, res.id)
        Assertions.assertEquals(found.authors, res.authors)
        Assertions.assertEquals(found.title, res.title)
        Assertions.assertEquals(found.isbn10, res.isbn10)
        Assertions.assertEquals(found.pageCount, res.pageCount)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
        Assertions.assertEquals(2, res.series?.size)
        val names = listOf(s1.name, s2.name)
        res.series?.forEach {
            Assertions.assertEquals(1.0, it.numberInSeries)
            Assertions.assertTrue(names.contains(it.name))
        }
        var entitiesIds = luceneHelper.searchEntitiesIds("tit", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:ser", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:series", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:(series 1)", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        val series1Id = res.series?.get(0)?.seriesId

        bookService.deleteSeriesFromBook(res.id!!, res.series?.get(0)?.seriesId!!)
        val afterDelete = bookService.findBookById(res.id!!)
        Assertions.assertEquals(1, afterDelete.series?.size)
        Assertions.assertEquals("series2", afterDelete.series?.get(0)?.name)
        Assertions.assertEquals(1.0, afterDelete.series?.get(0)?.numberInSeries)

        val seriesById = bookService.findSeriesById(series1Id!!)
        Assertions.assertEquals("series 1", seriesById.name)
        entitiesIds = luceneHelper.searchEntitiesIds("series:(series 1)", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
    }

    @Test
    fun testDeleteSeriesFromDbCascadesToBook() {
        val s1 = SeriesOrderDto(name = "series 1", numberInSeries = 1.0, seriesId = null)
        val s2 = SeriesOrderDto(name = "series2", numberInSeries = 1.0, seriesId = null)
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
                // seriesBak = "",
                authors = emptyList(),
                // numberInSeries = null,
                tags = emptyList(),
                goodreadsId = "",
                googleId = "",
                librarythingId = "",
                language = "",
                amazonId = "",
                series = listOf(s1, s2),
            ),
            null,
        )
        Assertions.assertNotNull(res.id)
        val found = bookService.findBookById(res.id!!)
        Assertions.assertEquals(found.id, res.id)
        Assertions.assertEquals(found.authors, res.authors)
        Assertions.assertEquals(found.title, res.title)
        Assertions.assertEquals(found.isbn10, res.isbn10)
        Assertions.assertEquals(found.pageCount, res.pageCount)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
        Assertions.assertEquals(2, res.series?.size)
        val names = listOf(s1.name, s2.name)
        res.series?.forEach {
            Assertions.assertEquals(1.0, it.numberInSeries)
            Assertions.assertTrue(names.contains(it.name))
        }
        var entitiesIds = luceneHelper.searchEntitiesIds("tit", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:ser", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:series", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:(series 1)", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        val series1Id = res.series?.get(0)?.seriesId

        bookService.deleteSeriesById(series1Id!!)
        val afterDelete = bookService.findBookById(res.id!!)
        Assertions.assertEquals(1, afterDelete.series?.size)
        Assertions.assertEquals("series2", afterDelete.series?.get(0)?.name)
        Assertions.assertEquals(1.0, afterDelete.series?.get(0)?.numberInSeries)

        Assertions.assertThrows(EntityNotFoundException::class.java) {
            bookService.findSeriesById(series1Id)
        }
        entitiesIds = luceneHelper.searchEntitiesIds("series:(series 1)", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
    }

    @Test
    fun testUpdateSeriesRebuildsIndex() {
        val saved = bookService.saveSeries(SeriesUpdateDto("series1"))
        Assertions.assertEquals("series1", saved.name)
        Assertions.assertNotNull(saved.id)
        val s1 = SeriesOrderDto(name = "series1", numberInSeries = 1.0)
        val s2 = SeriesOrderDto(name = "series2", numberInSeries = 1.0)
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
                // seriesBak = "",
                authors = emptyList(),
                // numberInSeries = null,
                tags = emptyList(),
                goodreadsId = "",
                googleId = "",
                librarythingId = "",
                language = "",
                amazonId = "",
                series = listOf(s1, s2),
            ),
            null,
        )
        Assertions.assertNotNull(res.id)
        val found = bookService.findBookById(res.id!!)
        Assertions.assertEquals(found.id, res.id)
        Assertions.assertEquals(found.authors, res.authors)
        Assertions.assertEquals(found.title, res.title)
        Assertions.assertEquals(found.isbn10, res.isbn10)
        Assertions.assertEquals(found.pageCount, res.pageCount)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
        Assertions.assertEquals(2, res.series?.size)
        val names = listOf(s1.name, s2.name)
        res.series?.forEach {
            Assertions.assertEquals(1.0, it.numberInSeries)
            Assertions.assertTrue(names.contains(it.name))
        }
        var entitiesIds = luceneHelper.searchEntitiesIds("tit", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
        entitiesIds = luceneHelper.searchEntitiesIds("series:ser", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))

        entitiesIds = luceneHelper.searchEntitiesIds("series:series1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))

        val updateSeries = bookService.updateSeries(saved.id!!, SeriesUpdateDto(name = "series3"))
        Assertions.assertEquals("series3", updateSeries.name)

        val search = bookService.findSeriesById(saved.id!!)
        Assertions.assertEquals("series3", search.name)
        entitiesIds = luceneHelper.searchEntitiesIds("series:series1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)

        entitiesIds = luceneHelper.searchEntitiesIds("series:series3", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
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
                // seriesBak = "",
                authors = emptyList(),
                // numberInSeries = null,
                tags = emptyList(),
                goodreadsId = "",
                googleId = "",
                librarythingId = "",
                language = "",
                amazonId = "",
            ),
            null,
        )
        Assertions.assertNotNull(res.id)
        val found = bookService.findBookById(res.id!!)
        Assertions.assertEquals(found.id, res.id)
        Assertions.assertEquals(found.authors, res.authors)
        Assertions.assertEquals(found.title, res.title)
        Assertions.assertEquals(found.isbn10, res.isbn10)
        Assertions.assertEquals(found.pageCount, res.pageCount)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
        val entitiesIds = luceneHelper.searchEntitiesIds("tit", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        Assertions.assertEquals(res.id, UUID.fromString(entitiesIds?.get(0)))
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
        Assertions.assertEquals(0, found.translators?.size)
        Assertions.assertEquals(found.title, res.title)
        Assertions.assertEquals(found.isbn10, res.isbn10)
        Assertions.assertEquals(found.pageCount, res.pageCount)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
        var entitiesIds = luceneHelper.searchEntitiesIds("author:test", LuceneEntity.Book)
        Assertions.assertEquals(2, entitiesIds?.size)
        bookService.deleteAuthorById(found.authors?.get(0)?.id!!)
        val foundAfterModification = bookService.findBookById(res.id!!)
        Assertions.assertEquals(0, foundAfterModification.authors?.size)
        val foundAfterModification2 = bookService.findBookById(res2.id!!)
        Assertions.assertEquals(0, foundAfterModification2.authors?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("author:test", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("tit", LuceneEntity.Book)
        Assertions.assertEquals(2, entitiesIds?.size)
    }

    @Test
    fun testDeletedAuthorShouldBeRemovedFromBookTranslators() {
        val dto = BookCreateDto(
            id = null,
            title = "title",
            isbn10 = "1566199093",
            isbn13 = "9781566199094 ",
            summary = "This is a test summary\nwith a newline",
            image = "",
            publisher = "test-publisher",
            pageCount = 50,
            publishedDate = "",
            // seriesBak = "",
            authors = mutableListOf(authorDto()),
            translators = mutableListOf(authorDto()),
            // numberInSeries = null,
            tags = emptyList(),
            goodreadsId = "4321abc",
            googleId = "1234",
            librarythingId = "",
            language = "",
            amazonId = "",
        )
        val res: BookDto = bookService.save(bookDto(), null)
        Assertions.assertNotNull(res.id)
        val res2: BookDto = bookService.save(bookDto("title2"), null)
        Assertions.assertNotNull(res2.id)
        val res3: BookDto = bookService.save(dto, null)
        Assertions.assertNotNull(res3.id)
        Assertions.assertEquals(1, res3.translators?.size)
        var entitiesIds = luceneHelper.searchEntitiesIds("author:test", LuceneEntity.Book)
        Assertions.assertEquals(3, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("translator:test", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        val found = bookService.findBookById(res.id!!)
        Assertions.assertEquals(found.id, res.id)
        Assertions.assertEquals(found.authors?.get(0)?.name, res.authors?.get(0)?.name)
        Assertions.assertEquals(0, found.translators?.size)
        Assertions.assertEquals(found.title, res.title)
        Assertions.assertEquals(found.isbn10, res.isbn10)
        Assertions.assertEquals(found.pageCount, res.pageCount)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
        val found1 = bookService.findBookById(res3.id!!)
        Assertions.assertEquals(found1.id, res3.id)
        Assertions.assertEquals(found1.authors?.get(0)?.name, res3.authors?.get(0)?.name)
        Assertions.assertEquals(found1.translators?.get(0)?.name, res3.translators?.get(0)?.name)
        Assertions.assertEquals(1, found1.translators?.size)
        Assertions.assertEquals(found1.title, res3.title)
        bookService.deleteAuthorById(found.authors?.get(0)?.id!!)
        val foundAfterModification = bookService.findBookById(res.id!!)
        Assertions.assertEquals(0, foundAfterModification.authors?.size)
        val foundAfterModification2 = bookService.findBookById(res2.id!!)
        Assertions.assertEquals(0, foundAfterModification2.authors?.size)
        val foundAfterModification3 = bookService.findBookById(res3.id!!)
        Assertions.assertEquals(0, foundAfterModification3.authors?.size)
        Assertions.assertEquals(0, foundAfterModification3.translators?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("author:test", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("translator:test", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
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
        var entitiesIds = luceneHelper.searchEntitiesIds("author:test", LuceneEntity.Book)
        Assertions.assertEquals(2, entitiesIds?.size)
        val authorId = res.authors?.get(0)?.id
        val authorName = res.authors?.get(0)?.name
        bookService.deleteAuthorFromBook(res.id!!, authorId!!)
        val foundAfterModification = bookService.findBookById(res.id!!)
        Assertions.assertEquals(0, foundAfterModification.authors?.size)
        val foundAfterModification2 = bookService.findBookById(res2.id!!)
        Assertions.assertEquals(1, foundAfterModification2.authors?.size)
        val authorStillInDb = bookService.findAuthorsById(authorId)
        Assertions.assertEquals(authorName, authorStillInDb.name)
        entitiesIds = luceneHelper.searchEntitiesIds("author:test", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
    }

    @Test
    fun testDeleteTranslatorOnlyFromOneBookStillExistsInDb() {
        val dto = BookCreateDto(
            id = null,
            title = "title",
            isbn10 = "1566199093",
            isbn13 = "9781566199094 ",
            summary = "This is a test summary\nwith a newline",
            image = "",
            publisher = "test-publisher",
            pageCount = 50,
            publishedDate = "",
            // seriesBak = "",
            authors = mutableListOf(authorDto()),
            translators = mutableListOf(authorDto()),
            // numberInSeries = null,
            tags = emptyList(),
            goodreadsId = "4321abc",
            googleId = "1234",
            librarythingId = "",
            language = "",
            amazonId = "",
        )
        val res: BookDto = bookService.save(dto, null)
        Assertions.assertNotNull(res.id)
        Assertions.assertEquals(1, res.translators?.size)
        val res2: BookDto = bookService.save(bookDto("title2"), null)
        Assertions.assertNotNull(res2.id)
        var entitiesIds = luceneHelper.searchEntitiesIds("author:test", LuceneEntity.Book)
        Assertions.assertEquals(2, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("translator:test", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        val found = bookService.findBookById(res.id!!)
        Assertions.assertEquals(found.id, res.id)
        Assertions.assertEquals(found.authors?.get(0)?.name, res.authors?.get(0)?.name)
        Assertions.assertEquals(found.translators?.get(0)?.name, res.translators?.get(0)?.name)
        Assertions.assertEquals(found.title, res.title)
        Assertions.assertEquals(found.isbn10, res.isbn10)
        Assertions.assertEquals(found.pageCount, res.pageCount)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
        val authorId = res.translators?.get(0)?.id
        val authorName = res.translators?.get(0)?.name
        bookService.deleteTranslatorFromBook(res.id!!, authorId!!)
        val foundAfterModification = bookService.findBookById(res.id!!)
        Assertions.assertEquals(1, foundAfterModification.authors?.size)
        Assertions.assertEquals(0, foundAfterModification.translators?.size)
        val foundAfterModification2 = bookService.findBookById(res2.id!!)
        Assertions.assertEquals(1, foundAfterModification2.authors?.size)
        val authorStillInDb = bookService.findAuthorsById(authorId)
        Assertions.assertEquals(authorName, authorStillInDb.name)
        entitiesIds = luceneHelper.searchEntitiesIds("author:test", LuceneEntity.Book)
        Assertions.assertEquals(2, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("translator:test", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
    }

    @Test
    fun testInsertUserbookWithNewBookNoImage() {
        var entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        val createBook = bookDto()
        val createUserBookDto = createUserBookDto(createBook, borrowed = true)
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
        Assertions.assertEquals(createUserBookDto.borrowed, saved.borrowed)
        Assertions.assertEquals(createUserBookDto.toRead, saved.toRead)
        Assertions.assertEquals(createUserBookDto.personalNotes, saved.personalNotes)
        Assertions.assertEquals(createUserBookDto.percentRead, saved.percentRead)
        Assertions.assertEquals(createUserBookDto.currentPageNumber, saved.currentPageNumber)
        Assertions.assertNotNull(saved.creationDate)
        Assertions.assertNotNull(saved.modificationDate)
        Assertions.assertNotNull(saved.book.creationDate)
        Assertions.assertNotNull(saved.book.modificationDate)
        Assertions.assertNull(saved.book.image)
        Assertions.assertNull(saved.lastReadingEvent)
        Assertions.assertNull(saved.lastReadingEventDate)
        Assertions.assertTrue(readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).isEmpty)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
    }

    @Test
    fun testUpdateUserbookWithPercentReadAndPageNumber() {
        var entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        val createBook = bookDto()
        createBook.pageCount = null
        val createUserBookDto = createUserBookDto(createBook, borrowed = true)
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
        Assertions.assertEquals(createUserBookDto.borrowed, saved.borrowed)
        Assertions.assertEquals(createUserBookDto.toRead, saved.toRead)
        Assertions.assertEquals(createUserBookDto.personalNotes, saved.personalNotes)
        Assertions.assertEquals(createUserBookDto.percentRead, saved.percentRead)
        Assertions.assertEquals(createUserBookDto.currentPageNumber, saved.currentPageNumber)
        Assertions.assertNotNull(saved.creationDate)
        Assertions.assertNotNull(saved.modificationDate)
        Assertions.assertNotNull(saved.book.creationDate)
        Assertions.assertNotNull(saved.book.modificationDate)
        Assertions.assertNull(saved.book.image)
        Assertions.assertNull(saved.lastReadingEvent)
        Assertions.assertNull(saved.lastReadingEventDate)
        Assertions.assertTrue(readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).isEmpty)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)

        var update = UserBookUpdateDto(percentRead = 50, book = null, toRead = null, currentPageNumber = null, lastReadingEvent = null, owned = null, personalNotes = null, borrowed = null)
        var updated = bookService.update(saved.id!!, update)
        Assertions.assertEquals(update.percentRead, updated.percentRead)
        Assertions.assertEquals(update.currentPageNumber, updated.currentPageNumber)
        Assertions.assertNull(updated.book.pageCount)

        update = UserBookUpdateDto(percentRead = null, book = null, toRead = null, currentPageNumber = 20, lastReadingEvent = null, owned = null, personalNotes = null, borrowed = null)
        updated = bookService.update(saved.id!!, update)
        Assertions.assertEquals(50, updated.percentRead)
        Assertions.assertEquals(update.currentPageNumber, updated.currentPageNumber)
        Assertions.assertNull(updated.book.pageCount)

        update = UserBookUpdateDto(percentRead = null, book = BookCreateDto(pageCount = 100), toRead = null, currentPageNumber = null, lastReadingEvent = null, owned = null, personalNotes = null, borrowed = null)
        updated = bookService.update(saved.id!!, update)
        Assertions.assertEquals(50, updated.percentRead)
        Assertions.assertEquals(20, updated.currentPageNumber)
        Assertions.assertEquals(update.book?.pageCount, updated.book.pageCount)

        update = UserBookUpdateDto(percentRead = null, book = null, toRead = null, currentPageNumber = 40, lastReadingEvent = null, owned = null, personalNotes = null, borrowed = null)
        updated = bookService.update(saved.id!!, update)
        Assertions.assertEquals(40, updated.percentRead)
        Assertions.assertEquals(40, updated.currentPageNumber)
        Assertions.assertEquals(100, updated.book.pageCount)

        update = UserBookUpdateDto(percentRead = null, book = null, toRead = null, currentPageNumber = 100, lastReadingEvent = null, owned = null, personalNotes = null, borrowed = null)
        updated = bookService.update(saved.id!!, update)
        Assertions.assertEquals(100, updated.percentRead)
        Assertions.assertEquals(100, updated.currentPageNumber)
        Assertions.assertEquals(100, updated.book.pageCount)

        update = UserBookUpdateDto(percentRead = null, book = null, toRead = null, currentPageNumber = 0, lastReadingEvent = null, owned = null, personalNotes = null, borrowed = null)
        updated = bookService.update(saved.id!!, update)
        Assertions.assertEquals(0, updated.percentRead)
        Assertions.assertEquals(0, updated.currentPageNumber)
        Assertions.assertEquals(100, updated.book.pageCount)
    }

    @Test
    fun testQueryByOwnedField() {
        val createBook = bookDto("title owned")
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

        val createBook1 = bookDto("title not owned")
        val createUserBookDto1 = createUserBookDto(createBook1, owned = false)
        val saved1: UserBookLightDto = bookService.save(createUserBookDto1, user(), null)
        Assertions.assertEquals(createBook1.title, saved1.book.title)
        Assertions.assertEquals(createBook1.isbn10, saved1.book.isbn10)
        Assertions.assertEquals(createBook1.isbn13?.trim(), saved1.book.isbn13)
        Assertions.assertEquals("This is a test summary with a newline", saved1.book.summary)
        Assertions.assertEquals(createBook1.publisher, saved1.book.publisher)
        Assertions.assertEquals(createBook1.pageCount, saved1.book.pageCount)
        Assertions.assertEquals(createBook1.goodreadsId, saved1.book.goodreadsId)
        Assertions.assertNull(saved1.book.librarythingId)
        Assertions.assertEquals(createUserBookDto1.owned, saved1.owned)

        val createBook2 = bookDto("owned is null")
        val createUserBookDto2 = createUserBookDto(createBook2, owned = null)
        val saved2: UserBookLightDto = bookService.save(createUserBookDto2, user(), null)
        Assertions.assertEquals(createBook2.title, saved2.book.title)
        Assertions.assertEquals(createBook2.isbn10, saved2.book.isbn10)
        Assertions.assertEquals(createBook2.isbn13?.trim(), saved2.book.isbn13)
        Assertions.assertEquals("This is a test summary with a newline", saved2.book.summary)
        Assertions.assertEquals(createBook2.publisher, saved2.book.publisher)
        Assertions.assertEquals(createBook2.pageCount, saved2.book.pageCount)
        Assertions.assertEquals(createBook2.goodreadsId, saved2.book.goodreadsId)
        Assertions.assertNull(saved2.book.librarythingId)
        Assertions.assertEquals(createUserBookDto2.owned, saved2.owned)

        var nb = bookService.findUserBookByCriteria(user().id.value, null, null, null, null, null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(3, nb)

        var res = bookService.findUserBookByCriteria(user().id.value, null, null, null, true, null, Pageable.ofSize(30))
        nb = res.totalElements
        Assertions.assertEquals(1, nb)
        Assertions.assertEquals(createBook.title, res.content[0].book.title)

        res = bookService.findUserBookByCriteria(user().id.value, null, null, null, false, null, Pageable.ofSize(30))
        nb = res.totalElements
        Assertions.assertEquals(2, nb)
        val titles = listOf<String>(createBook1.title, createBook2.title)
        res.content.forEach {
            Assertions.assertTrue(titles.contains(it.book.title))
        }
    }

    @Test
    fun testQueryWithRandomOrder() {
        // Create books to validate response
        val targetPageableSize = 24
        val booksToCreate = targetPageableSize + 4
        for (bookNumber in 1..booksToCreate) {
            val createBook = bookDto("title $bookNumber")
            val createUserBookDto = createUserBookDto(createBook)
            bookService.save(createUserBookDto, user(), null)
        }

        val totalCheckRes = bookService.findUserBookByCriteria(user().id.value, null, null, null, null, null, Pageable.ofSize(booksToCreate))
        // Check total number of books created -- cast to Int for assert
        val totalNumberOfBooks = totalCheckRes.totalElements.toInt()
        Assertions.assertEquals(booksToCreate, totalNumberOfBooks)

        val pageable = PageRequest.of(0, targetPageableSize, Sort.by("random").descending())
        val randomCheckRes = bookService.findUserBookByCriteria(user().id.value, null, null, null, null, null, pageable)
        // Check number of randomly returned books (pageSize, not total)
        val randomNumberOfBooks = randomCheckRes.numberOfElements.toInt()
        Assertions.assertEquals(targetPageableSize, randomNumberOfBooks)
    }

    @Test
    fun testInsertUserbookWithNewBookImageAndEvent() {
        var entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
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
        Assertions.assertEquals(1, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
    }

    @Test
    fun testFindTagBooksFilterByEvent() {
        var entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        val createBook = bookDto(withTags = true)
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
        Assertions.assertEquals(1, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        val tagId = bookService.findAllTags("fantasy", Pageable.ofSize(8)).content[0].id!!
        val eventTypes: MutableList<ReadingEventType> = mutableListOf()
        var books = bookService.findTagBooksById(tagId, user(), Pageable.ofSize(8), LibraryFilter.ANY, eventTypes)
        Assertions.assertEquals(1, books.totalElements)
        eventTypes.add(ReadingEventType.DROPPED)
        books = bookService.findTagBooksById(tagId, user(), Pageable.ofSize(8), LibraryFilter.ANY, eventTypes)
        Assertions.assertEquals(0, books.totalElements)
        eventTypes.add(ReadingEventType.CURRENTLY_READING)
        books = bookService.findTagBooksById(tagId, user(), Pageable.ofSize(8), LibraryFilter.ANY, eventTypes)
        Assertions.assertEquals(1, books.totalElements)
    }

    @Test
    fun testInsertUserbookWithExistingBookNoImage() {
        var entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        val createBook = bookDto()
        val savedBook = bookService.save(createBook, null)
        Assertions.assertEquals(createBook.title, savedBook.title)
        Assertions.assertNotNull(savedBook.id)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        val modified = BookCreateDto(
            savedBook.id,
            title = "modified title",
            isbn10 = savedBook.isbn10,
            isbn13 = savedBook.isbn13,
        )
        val createUserBookDto = createUserBookDto(modified)
        val saved: UserBookLightDto = bookService.save(createUserBookDto, user(), null)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("title", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
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
        Assertions.assertTrue(readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).isEmpty)
        Assertions.assertNull(saved.book.image)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
    }

    @Test
    fun testInsertUserbookWithExistingBookAndExistingBookHasImage() {
        var entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        val createBook = bookDto()
        val uploadFile = MockMultipartFile("test-cover.jpg", "test-cover.jpg", "image/jpeg", this::class.java.getResourceAsStream("test-cover.jpg"))
        val savedBook = bookService.save(createBook, uploadFile)
        Assertions.assertEquals(createBook.title, savedBook.title)
        Assertions.assertNotNull(savedBook.id)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        val modified = BookCreateDto(
            savedBook.id,
            title = "modified title",
            isbn10 = savedBook.isbn10,
            isbn13 = savedBook.isbn13,
        )
        val createUserBookDto = createUserBookDto(modified)
        val saved: UserBookLightDto = bookService.save(createUserBookDto, user(), null)
        entitiesIds = luceneHelper.searchEntitiesIds("title", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
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
        Assertions.assertTrue(readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).isEmpty)
        Assertions.assertTrue(saved.book.image!!.contains(slugify(savedBook.title), true))
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
    }

    @Test
    fun testInsertUserbookWithImageAndExistingBookAndExistingBookHasImage() {
        var entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        val createBook = bookDto()
        val uploadFile = MockMultipartFile("test-cover.jpg", "test-cover.jpg", "image/jpeg", this::class.java.getResourceAsStream("test-cover.jpg"))
        val savedBook = bookService.save(createBook, uploadFile)
        Assertions.assertEquals(createBook.title, savedBook.title)
        Assertions.assertNotNull(savedBook.id)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        val modified = BookCreateDto(
            savedBook.id,
            title = "modified title",
            isbn10 = savedBook.isbn10,
            isbn13 = savedBook.isbn13,
        )
        val createUserBookDto = createUserBookDto(modified)
        val replacementFile = MockMultipartFile("test-replace-cover.jpg", "test-replace-cover.jpg", "image/jpeg", this::class.java.getResourceAsStream("test-cover.jpg"))
        val saved: UserBookLightDto = bookService.save(createUserBookDto, user(), replacementFile)
        entitiesIds = luceneHelper.searchEntitiesIds("title", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
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
        Assertions.assertTrue(readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).isEmpty)
        Assertions.assertTrue(saved.book.image!!.contains(slugify(modified.title), true))
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
    }

    @Test
    fun testUpdateUserbookWithImageAndEventNoNewEvent() {
        var entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
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
        Assertions.assertEquals(1, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)

        val updater = UserBookUpdateDto(
            ReadingEventType.FINISHED,
            personalNotes = "new notes",
            owned = false,
            book = null,
            toRead = null,
            percentRead = 50,
            borrowed = null,
            currentPageNumber = null,
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
        Assertions.assertEquals(1, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
    }

    @Test
    fun testUpdateUserbookWithImageAndEventNewEventRequired() {
        var entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
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
        Assertions.assertEquals(1, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)

        val updater = UserBookUpdateDto(
            ReadingEventType.DROPPED,
            personalNotes = "new notes",
            owned = false,
            book = null,
            toRead = null,
            percentRead = 50,
            borrowed = true,
            currentPageNumber = null,
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
        Assertions.assertEquals(updater.borrowed, updated.borrowed)
        Assertions.assertNotNull(updated.creationDate)
        Assertions.assertNotNull(updated.modificationDate)
        Assertions.assertNotNull(updated.book.creationDate)
        Assertions.assertNotNull(updated.book.modificationDate)
        Assertions.assertTrue(updated.book.image!!.contains(slugify(updated.book.title), true))
        Assertions.assertEquals(ReadingEventType.DROPPED, updated.lastReadingEvent)
        Assertions.assertNotNull(updated.lastReadingEventDate)
        Assertions.assertEquals(2, updated.readingEvents?.size)
        Assertions.assertEquals(2, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
    }

    @Test
    fun testUpdateUserbookWithImageAndEventNewEventRequiredAndDeleteExistingImage() {
        var entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        val createBook = bookDto()
        val createUserBookDto = createUserBookDto(createBook, ReadingEventType.FINISHED, nowInstant(), borrowed = true)
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
        Assertions.assertEquals(1, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)

        val updater = UserBookUpdateDto(
            ReadingEventType.DROPPED,
            personalNotes = "new notes",
            owned = false,
            book = createBook.copy(image = null),
            toRead = null,
            percentRead = 50,
            borrowed = false,
            currentPageNumber = null,
        )
        // val replacementFile = MockMultipartFile("test-replace-cover.jpg", "test-replace-cover.jpg", "image/jpeg", this::class.java.getResourceAsStream("test-cover.jpg"))
        val updated = bookService.update(saved.id!!, updater, null)
        Assertions.assertEquals(createBook.title, updated.book.title)
        Assertions.assertEquals(createBook.isbn10, updated.book.isbn10)
        Assertions.assertEquals(createBook.isbn13?.trim(), updated.book.isbn13)
        Assertions.assertEquals("This is a test summary with a newline", updated.book.summary)
        Assertions.assertEquals(createBook.publisher, updated.book.publisher)
        Assertions.assertEquals(createBook.pageCount, updated.book.pageCount)
        Assertions.assertEquals(createBook.goodreadsId, updated.book.goodreadsId)
        Assertions.assertEquals("", updated.book.librarythingId)
        Assertions.assertEquals(updater.owned, updated.owned)
        Assertions.assertEquals(saved.toRead, updated.toRead)
        Assertions.assertEquals(updater.percentRead, updated.percentRead)
        Assertions.assertEquals(updater.personalNotes, updated.personalNotes)
        Assertions.assertEquals(updater.borrowed, updated.borrowed)
        Assertions.assertNotNull(updated.creationDate)
        Assertions.assertNotNull(updated.modificationDate)
        Assertions.assertNotNull(updated.book.creationDate)
        Assertions.assertNotNull(updated.book.modificationDate)
        Assertions.assertNull(updated.book.image)
        Assertions.assertEquals(ReadingEventType.DROPPED, updated.lastReadingEvent)
        Assertions.assertNotNull(updated.lastReadingEventDate)
        Assertions.assertEquals(2, updated.readingEvents?.size)
        Assertions.assertEquals(2, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
    }

    @Test
    fun testUpdateUserbookWithImageAndEventNewEventRequiredAndNewImage() {
        var entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        val createBook = bookDto()
        val createUserBookDto = createUserBookDto(createBook, ReadingEventType.FINISHED, nowInstant(), borrowed = true)
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
        Assertions.assertEquals(1, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)

        val updater = UserBookUpdateDto(
            ReadingEventType.DROPPED,
            personalNotes = "new notes",
            owned = false,
            book = null,
            toRead = null,
            percentRead = 50,
            borrowed = null,
            currentPageNumber = null,
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
        Assertions.assertTrue(updated.borrowed!!)
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
        Assertions.assertEquals(2, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
    }

    @Test
    fun deleteUserBookRemovesEventsDoesNotRemoveBook() {
        var entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        val createBook = bookDto()
        val uploadFile = MockMultipartFile("test-cover.jpg", "test-cover.jpg", "image/jpeg", this::class.java.getResourceAsStream("test-cover.jpg"))
        val savedBook = bookService.save(createBook, uploadFile)
        Assertions.assertEquals(createBook.title, savedBook.title)
        Assertions.assertNotNull(savedBook.id)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        val modified = BookCreateDto(
            savedBook.id,
            title = "modified title",
            isbn10 = savedBook.isbn10,
            isbn13 = savedBook.isbn13,
        )
        val createUserBookDto = createUserBookDto(modified, ReadingEventType.FINISHED)
        val saved1: UserBookLightDto = bookService.save(createUserBookDto, user(), null)
        val second = BookCreateDto(id = savedBook.id)
        val createUserBookDto2 = createUserBookDto(second, ReadingEventType.FINISHED)
        val saved2: UserBookLightDto = bookService.save(createUserBookDto2, user(), null)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("title", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)

        Assertions.assertNotNull(saved1)
        Assertions.assertNotNull(saved2)
        var nb = bookService.findUserBookByCriteria(user().id.value, null, null, null, null, null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(2, nb)
        var eventsNb = readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(2, eventsNb)
        bookService.deleteUserBookById(saved1.id!!)
        nb = bookService.findUserBookByCriteria(user().id.value, null, null, null, null, null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(1, nb)
        eventsNb = readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(1, eventsNb)
    }

    @Test
    fun deleteBookCascades() {
        var entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        val createBook = bookDto(withTags = true)
        val uploadFile = MockMultipartFile("test-cover.jpg", "test-cover.jpg", "image/jpeg", this::class.java.getResourceAsStream("test-cover.jpg"))
        val savedBook = bookService.save(createBook, uploadFile)
        Assertions.assertEquals(createBook.title, savedBook.title)
        Assertions.assertNotNull(savedBook.id)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        val modified = BookCreateDto(
            savedBook.id,
            title = "modified title",
            isbn10 = savedBook.isbn10,
            isbn13 = savedBook.isbn13,
        )
        val createUserBookDto = createUserBookDto(modified, ReadingEventType.FINISHED)
        val saved1: UserBookLightDto = bookService.save(createUserBookDto, user(), null)
        val second = BookCreateDto(id = savedBook.id)
        val createUserBookDto2 = createUserBookDto(second, ReadingEventType.FINISHED)
        val saved2: UserBookLightDto = bookService.save(createUserBookDto2, user(), null)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("title", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)

        Assertions.assertNotNull(saved1)
        Assertions.assertNotNull(saved2)
        var nb = bookService.findUserBookByCriteria(user().id.value, null, null, null, null, null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(2, nb)
        var eventsNb = readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(2, eventsNb)
        var authorsNb = bookService.findAllAuthors(null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(1, authorsNb)
        var tagsNb = bookService.findAllTags(null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(2, tagsNb)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
        bookService.deleteBookById(savedBook.id!!)
        nb = bookService.findUserBookByCriteria(user().id.value, null, null, null, null, null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(0, nb)
        authorsNb = bookService.findAllAuthors(null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(1, authorsNb)
        eventsNb = readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(0, eventsNb)
        tagsNb = bookService.findAllTags(null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(2, tagsNb)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("title", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
    }

    @Test
    fun testRemoveTagFromOneBook() {
        var entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        val createBook = bookDto(withTags = true)
        val uploadFile = MockMultipartFile("test-cover.jpg", "test-cover.jpg", "image/jpeg", this::class.java.getResourceAsStream("test-cover.jpg"))
        val savedBook = bookService.save(createBook, uploadFile)
        Assertions.assertEquals(createBook.title, savedBook.title)
        Assertions.assertNotNull(savedBook.id)
        Assertions.assertEquals(1, File(jeluProperties.files.images).listFiles().size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        val modified = BookCreateDto(
            savedBook.id,
            title = "modified title",
            isbn10 = savedBook.isbn10,
            isbn13 = savedBook.isbn13,
        )
        val createUserBookDto = createUserBookDto(modified, ReadingEventType.FINISHED)
        val saved1: UserBookLightDto = bookService.save(createUserBookDto, user(), null)
        val createBook2 = bookDto(withTags = true)
        val savedBook2 = bookService.save(createBook2, uploadFile)
        Assertions.assertEquals(createBook.title, savedBook.title)
        Assertions.assertEquals(2, savedBook2.tags?.size)
        Assertions.assertEquals(2, saved1.book.tags?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("modified", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("tag:fantasy", LuceneEntity.Book)
        Assertions.assertEquals(2, entitiesIds?.size)
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
        entitiesIds = luceneHelper.searchEntitiesIds("tag:fantasy", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("modified", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
    }

    @Test
    fun testRemoveTagFromDb() {
        var entitiesIds = luceneHelper.searchEntitiesIds("tag:fantasy", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("modified", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
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
            isbn13 = savedBook.isbn13,
        )
        val createUserBookDto = createUserBookDto(modified, ReadingEventType.FINISHED)
        val saved1: UserBookLightDto = bookService.save(createUserBookDto, user(), null)
        val createBook2 = bookDto(withTags = true)
        val savedBook2 = bookService.save(createBook2, uploadFile)
        Assertions.assertEquals(createBook.title, savedBook.title)
        Assertions.assertEquals(2, savedBook2.tags?.size)
        Assertions.assertEquals(2, saved1.book.tags?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("tag:fantasy", LuceneEntity.Book)
        Assertions.assertEquals(2, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("modified", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
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
        entitiesIds = luceneHelper.searchEntitiesIds("tag:fantasy", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("modified", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
    }

    @Test
    fun testAddAndRemoveTagsToBook() {
        bookService.findAllTags(null, Pageable.ofSize(20)).content.forEach {
            bookService.deleteTagById(it.id!!)
        }
        val createBook = bookDto(withTags = true)
        val savedBook = bookService.save(createBook, null)
        Assertions.assertEquals(2, savedBook.tags?.size)
        var entitiesIds = luceneHelper.searchEntitiesIds("tag:fantasy", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("tag:tag1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("tag:another", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)

        val tag3 = bookService.save(tagDto("added tag1"))
        Assertions.assertEquals("added tag1", tag3.name)
        Assertions.assertNotNull(tag3.id)
        entitiesIds = luceneHelper.searchEntitiesIds("tag:fantasy", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("tag:tag1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("tag:another", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)

        val tag4 = bookService.save(tagDto("another tag"))
        Assertions.assertEquals("another tag", tag4.name)
        Assertions.assertNotNull(tag4.id)
        entitiesIds = luceneHelper.searchEntitiesIds("tag:fantasy", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("tag:tag1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("tag:another", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)

        val tags = bookService.findAllTags(null, Pageable.ofSize(20))
        Assertions.assertEquals(4, tags.totalElements)
        var orphanTags = bookService.findOrphanTags(PageRequest.of(0, 20))
        Assertions.assertEquals(2, orphanTags.totalElements)

        val nb = bookService.addTagsToBook(savedBook.id!!, listOf(tag3.id!!, tag4.id!!))
        Assertions.assertEquals(2, nb)
        entitiesIds = luceneHelper.searchEntitiesIds("tag:fantasy", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("tag:tag1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("tag:another", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)

        orphanTags = bookService.findOrphanTags(PageRequest.of(0, 20))
        Assertions.assertEquals(0, orphanTags.totalElements)

        val after = bookService.findBookById(savedBook.id!!)
        Assertions.assertEquals(4, after.tags?.size)

        bookService.deleteTagsFromBook(after.id!!, listOf(tag3.id!!, tag4.id!!))
        val afterDelete = bookService.findBookById(after.id!!)
        Assertions.assertEquals(2, afterDelete.tags?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("tag:fantasy", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("tag:tag1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("tag:another", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        orphanTags = bookService.findOrphanTags(PageRequest.of(0, 20))
        Assertions.assertEquals(2, orphanTags.totalElements)
    }

    @Test
    fun testFindOrphanTags() {
        bookService.findAllTags(null, Pageable.ofSize(20)).content.forEach {
            bookService.deleteTagById(it.id!!)
        }
        val createBook = bookDto(withTags = true)
        val savedBook = bookService.save(createBook, null)
        Assertions.assertEquals(2, savedBook.tags?.size)
        var entitiesIds = luceneHelper.searchEntitiesIds("tag:fantasy", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("tag:tag1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("tag:another", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)

        val tag3 = bookService.save(tagDto("added tag1"))
        Assertions.assertEquals("added tag1", tag3.name)
        Assertions.assertNotNull(tag3.id)
        entitiesIds = luceneHelper.searchEntitiesIds("tag:fantasy", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("tag:tag1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("tag:another", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)

        val tag4 = bookService.save(tagDto("another tag"))
        Assertions.assertEquals("another tag", tag4.name)
        Assertions.assertNotNull(tag4.id)
        entitiesIds = luceneHelper.searchEntitiesIds("tag:fantasy", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("tag:tag1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("tag:another", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("title1", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)

        val tags = bookService.findAllTags(null, Pageable.ofSize(20))
        Assertions.assertEquals(4, tags.totalElements)

        val orphanTags = bookService.findOrphanTags(PageRequest.of(0, 20))
        Assertions.assertEquals(2, orphanTags.totalElements)
        orphanTags.content.forEach { t -> Assertions.assertTrue(t.name == tag3.name || t.name == tag4.name) }
    }

    @Test
    fun testMergeAuthors() {
        var entitiesIds = luceneHelper.searchEntitiesIds("book", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("author:author1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("author:author2", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
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
            // seriesBak = "",
            authors = mutableListOf(authorDto1),
            translators = mutableListOf(authorDto2),
            // numberInSeries = null,
            tags = tags(),
            goodreadsId = "4321abc",
            googleId = "1234",
            librarythingId = "",
            language = "",
            amazonId = "",
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
            // seriesBak = "",
            authors = mutableListOf(authorDto2),
            // numberInSeries = null,
            tags = tags(),
            goodreadsId = "4321abc",
            googleId = "1234",
            librarythingId = "",
            language = "",
            amazonId = "",
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
            // seriesBak = "",
            authors = mutableListOf(authorDto2, authorDto1),
            // numberInSeries = null,
            tags = tags(),
            goodreadsId = "4321abc",
            googleId = "1234",
            librarythingId = "",
            language = "",
            amazonId = "",
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
        entitiesIds = luceneHelper.searchEntitiesIds("book", LuceneEntity.Book)
        Assertions.assertEquals(3, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("author:author1", LuceneEntity.Book)
        Assertions.assertEquals(2, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("author:author2", LuceneEntity.Book)
        Assertions.assertEquals(2, entitiesIds?.size)

        val authorId1 = savedBook1.authors?.get(0)?.id
        val authorId2 = savedBook2.authors?.get(0)?.id
        val author1BooksNb = bookService.findAuthorBooksById(authorId1!!, user(), Pageable.ofSize(30), LibraryFilter.ANY).totalElements
        val author2BooksNb = bookService.findAuthorBooksById(authorId2!!, user(), Pageable.ofSize(30), LibraryFilter.ANY).totalElements
        Assertions.assertEquals(2, author1BooksNb)
        Assertions.assertEquals(3, author2BooksNb)
        var authorsNb = bookService.findAllAuthors(null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(2, authorsNb)
        val update = AuthorUpdateDto(biography = null, name = "author3", dateOfBirth = "2000-12-12", dateOfDeath = null, facebookPage = null, goodreadsPage = null, image = null, instagramPage = null, officialPage = null, twitterPage = null, wikipediaPage = null, notes = null, creationDate = null, id = null, modificationDate = null)
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
        val book1AfterMerge = bookService.findBookById(savedBook1.id!!)
        Assertions.assertEquals(1, book1AfterMerge.translators?.size)
        Assertions.assertEquals(1, book1AfterMerge.authors?.size)

        entitiesIds = luceneHelper.searchEntitiesIds("book", LuceneEntity.Book)
        Assertions.assertEquals(3, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("author:author1", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("author:author2", LuceneEntity.Book)
        Assertions.assertEquals(0, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("author:author3", LuceneEntity.Book)
        Assertions.assertEquals(3, entitiesIds?.size)
    }

    fun user(): User {
        val userDetail = userService.loadUserByUsername("testuser")
        return (userDetail as JeluUser).user
    }
}
