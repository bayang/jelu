package io.github.bayang.jelu.service

import io.github.bayang.jelu.authorDto
import io.github.bayang.jelu.dto.BookCreateDto
import io.github.bayang.jelu.dto.CreateUserDto
import io.github.bayang.jelu.dto.CustomListDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.dto.LibraryFilter
import io.github.bayang.jelu.dto.TagDto
import io.github.bayang.jelu.dto.UserDto
import io.github.bayang.jelu.search.LuceneEntity
import io.github.bayang.jelu.search.LuceneHelper
import io.github.bayang.jelu.tagDto
import org.apache.lucene.index.Term
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomListServiceTest(
    @Autowired private val customListService: CustomListService,
    @Autowired private val userService: UserService,
    @Autowired private val bookService: BookService,
    @Autowired private val luceneHelper: LuceneHelper,
) {
    @BeforeAll
    fun setupUser() {
        userService.save(CreateUserDto(login = "testuser", password = "1234", isAdmin = true))
    }

    @AfterAll
    fun teardDown() {
        userService.findAll(null).forEach { userService.deleteUser(it.id!!) }
    }

    @AfterEach
    fun cleanTest() {
        bookService
            .findUserBookByCriteria(user().id!!, null, null, null, null, null, Pageable.ofSize(100))
            .forEach { bookService.deleteUserBookById(it.id!!) }
        bookService.findAllAuthors(null, pageable = Pageable.ofSize(30)).forEach {
            bookService.deleteAuthorById(it.id!!)
        }
        bookService.findAllTags(null, Pageable.ofSize(20)).content.forEach {
            bookService.deleteTagById(it.id!!)
        }
        bookService.findAllSeries(null, null, Pageable.ofSize(20)).content.forEach {
            bookService.deleteSeriesById(it.id!!)
        }
        bookService.findAll(null, Pageable.ofSize(100), user(), null, null, null, null, LibraryFilter.ANY).forEach {
            bookService.deleteBookById(it.id!!)
        }
        luceneHelper.getIndexWriter().use { indexWriter ->
            indexWriter.deleteDocuments(Term(LuceneEntity.TYPE, LuceneEntity.Book.type))
            indexWriter.deleteDocuments(Term(LuceneEntity.TYPE, LuceneEntity.Author.type))
        }
    }

    @Test
    fun testCreateList() {
        val customListDto =
            CustomListDto(
                name = "the list",
                tags = "education,test space",
                actionable = false,
                public = true,
                id = null,
                creationDate = null,
                modificationDate = null,
            )
        val created = customListService.save(customListDto, user().id!!)
        Assertions.assertEquals(customListDto.name, created.name)
        Assertions.assertEquals(customListDto.tags, created.tags)
        Assertions.assertEquals(customListDto.actionable, created.actionable)
        Assertions.assertEquals(customListDto.public, created.public)
        Assertions.assertNotNull(created.id)
        Assertions.assertNotNull(created.modificationDate)
        Assertions.assertNotNull(created.creationDate)
        val creationDate = created.creationDate
        val updateDto =
            CustomListDto(
                name = customListDto.name,
                tags = "education,test space",
                actionable = true,
                public = false,
                id = created.id,
                creationDate = created.creationDate,
                modificationDate = null,
            )
        val updated = customListService.update(updateDto)
        Assertions.assertEquals(updateDto.name, updated.name)
        Assertions.assertEquals(updateDto.tags, updated.tags)
        Assertions.assertEquals(updateDto.public, updated.public)
        Assertions.assertEquals(updateDto.actionable, updated.actionable)
        Assertions.assertTrue(updated.modificationDate!!.isAfter(creationDate))

        val byId = customListService.findById(updated.id!!)
        Assertions.assertEquals(updated.name, byId.name)
        Assertions.assertEquals(updated.tags, byId.tags)
        Assertions.assertEquals(updated.public, byId.public)
        Assertions.assertEquals(updated.actionable, byId.actionable)

        var byUser = customListService.find(user().id!!, null, PageRequest.of(0, 20))
        Assertions.assertEquals(1, byUser.totalElements)
        val only = byUser.content.first()
        Assertions.assertEquals(updated.name, only.name)
        Assertions.assertEquals(updated.tags, only.tags)
        Assertions.assertEquals(updated.public, only.public)
        Assertions.assertEquals(updated.actionable, only.actionable)

        customListService.delete(updated.id!!)
        byUser = customListService.find(user().id!!, null, PageRequest.of(0, 20))
        Assertions.assertEquals(0, byUser.totalElements)
    }

    @Test
    fun testFetchListBooks() {
        val customListDto =
            CustomListDto(
                name = "the list",
                tags = "History,owned-physical",
                actionable = false,
                public = true,
                id = null,
                creationDate = null,
                modificationDate = null,
            )
        val created = customListService.save(customListDto, user().id!!)
        Assertions.assertEquals(customListDto.name, created.name)
        Assertions.assertEquals(customListDto.tags, created.tags)
        Assertions.assertEquals(customListDto.actionable, created.actionable)
        Assertions.assertEquals(customListDto.public, created.public)
        Assertions.assertNotNull(created.id)
        Assertions.assertNotNull(created.modificationDate)
        Assertions.assertNotNull(created.creationDate)

        var books = customListService.findListBooks(created.id!!, PageRequest.of(20, 20), null)
        Assertions.assertEquals(0, books.totalElements)

        val tags = mutableListOf<TagDto>()
        tags.add(tagDto("History"))
        tags.add(tagDto("owned-physical"))
        val createBook =
            BookCreateDto(
                id = null,
                title = "title 1",
                isbn10 = "1566199093",
                isbn13 = "9781566199094 ",
                summary = "This is a test summary\nwith a newline",
                image = "",
                publisher = "test-publisher",
                pageCount = 50,
                publishedDate = "",
                // seriesBak = "",
                authors = mutableListOf(authorDto()),
                // numberInSeries = null,
                tags = tags,
                goodreadsId = "4321abc",
                googleId = "1234",
                librarythingId = "",
                language = "",
                amazonId = "",
            )
        val savedBook1 = bookService.save(createBook, null)
        Assertions.assertEquals(2, savedBook1.tags?.size)
        var entitiesIds = luceneHelper.searchEntitiesIds("tag:History", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        entitiesIds = luceneHelper.searchEntitiesIds("tag:owned-physical", LuceneEntity.Book)
        Assertions.assertEquals(1, entitiesIds?.size)
        books = customListService.findListBooks(created.id!!, PageRequest.of(20, 20), null)
        Assertions.assertEquals(1, books.totalElements)

        val createBook1 =
            BookCreateDto(
                id = null,
                title = "title 2",
                isbn10 = "1566199093",
                isbn13 = "9781566199094 ",
                summary = "This is a test summary\nwith a newline",
                image = "",
                publisher = "test-publisher",
                pageCount = 50,
                publishedDate = "",
                // seriesBak = "",
                authors = mutableListOf(authorDto()),
                // numberInSeries = null,
                tags = tags.drop(1),
                goodreadsId = "4321abc",
                googleId = "1234",
                librarythingId = "",
                language = "",
                amazonId = "",
            )
        val savedBook2 = bookService.save(createBook1, null)
        Assertions.assertEquals(1, savedBook2.tags?.size)

        books = customListService.findListBooks(created.id!!, PageRequest.of(20, 20), null)
        Assertions.assertEquals(1, books.totalElements)

        val moreTags = mutableListOf<TagDto>()
        moreTags.add(tagDto("History"))
        moreTags.add(tagDto("owned-physical"))
        moreTags.add(tagDto("other_tag"))
        val createBook2 =
            BookCreateDto(
                id = null,
                title = "title 3",
                isbn10 = "1566199093",
                isbn13 = "9781566199094 ",
                summary = "This is a test summary\nwith a newline",
                image = "",
                publisher = "test-publisher",
                pageCount = 50,
                publishedDate = "",
                // seriesBak = "",
                authors = mutableListOf(authorDto()),
                // numberInSeries = null,
                tags = moreTags,
                goodreadsId = "4321abc",
                googleId = "1234",
                librarythingId = "",
                language = "",
                amazonId = "",
            )
        val savedBook3 = bookService.save(createBook2, null)
        Assertions.assertEquals(3, savedBook3.tags?.size)

        books = customListService.findListBooks(created.id!!, PageRequest.of(20, 20), null)
        Assertions.assertEquals(2, books.totalElements)
    }

    fun user(username: String = "testuser"): UserDto {
        val userDetail = userService.loadUserByUsername(username)
        return (userDetail as JeluUser).user
    }
}
