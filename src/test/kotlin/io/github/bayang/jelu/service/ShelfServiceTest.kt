package io.github.bayang.jelu.service

import io.github.bayang.jelu.dto.CreateShelfDto
import io.github.bayang.jelu.dto.CreateUserDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.dto.TagDto
import io.github.bayang.jelu.dto.UserDto
import io.github.bayang.jelu.errors.JeluValidationException
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Pageable
import java.util.UUID

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShelfServiceTest(
    @Autowired private val shelfService: ShelfService,
    @Autowired private val userService: UserService,
    @Autowired private val bookService: BookService,
) {

    @BeforeAll
    fun setupUser() {
        userService.save(CreateUserDto(login = "testuser", password = "1234", isAdmin = true))
    }

    @AfterAll
    fun teardDown() {
        shelfService.find(user(), null, null)
            .forEach { shelfService.delete(it.id!!) }
        userService.findAll(null).forEach { userService.deleteUser(it.id!!) }
    }

    @Test
    fun testSaveFindDelete() {
        shelfService.find(user(), null, null)
            .forEach { shelfService.delete(it.id!!) }
        val saved = shelfService.save(CreateShelfDto("to-buy", UUID.randomUUID()), user())
        Assertions.assertEquals("to-buy", saved.name)
        Assertions.assertNotNull(saved.creationDate)
        Assertions.assertNotNull(saved.modificationDate)
        Assertions.assertNotNull(saved.id)
        Assertions.assertNotNull(saved.targetId)

        var shelves = shelfService.find(user(), null, null)
        Assertions.assertEquals(1, shelves.size)
        Assertions.assertEquals("to-buy", shelves[0].name)

        val uuid = UUID.randomUUID()
        val saved1 = shelfService.save(CreateShelfDto("goncourt_2022", uuid), user())
        Assertions.assertEquals("goncourt_2022", saved1.name)
        Assertions.assertNotNull(saved1.creationDate)
        Assertions.assertNotNull(saved1.modificationDate)
        Assertions.assertNotNull(saved1.id)
        Assertions.assertNotNull(saved1.targetId)

        shelves = shelfService.find(user(), null, null)
        Assertions.assertEquals(2, shelves.size)

        shelves = shelfService.find(user(), "gonc", null)
        Assertions.assertEquals(1, shelves.size)
        Assertions.assertEquals("goncourt_2022", shelves[0].name)

        shelves = shelfService.find(user(), null, uuid)
        Assertions.assertEquals(1, shelves.size)
        Assertions.assertEquals("goncourt_2022", shelves[0].name)

        shelfService.delete(saved1.id!!)
        shelves = shelfService.find(user(), null, null)
        Assertions.assertEquals(1, shelves.size)
        Assertions.assertEquals("to-buy", shelves[0].name)
        shelves = shelfService.find(user(), "gonc", null)
        Assertions.assertEquals(0, shelves.size)
    }

    @Test
    fun testTooManyShelves() {
        shelfService.find(user(), null, null)
            .forEach { shelfService.delete(it.id!!) }
        for (i in 1..10) {
            val saved = shelfService.save(CreateShelfDto("shelf-$i", UUID.randomUUID()), user())
        }
        val shelves = shelfService.find(user(), null, null)
        Assertions.assertEquals(10, shelves.size)
        assertThrows<JeluValidationException> { shelfService.save(CreateShelfDto("shelf-11", UUID.randomUUID()), user()) }
    }

    @Test
    fun testDeletingTagDeletesCorrespondingShelves() {
        shelfService.find(user(), null, null)
            .forEach { shelfService.delete(it.id!!) }
        var tags = bookService.findAllTags(null, Pageable.ofSize(200))
        tags.forEach { bookService.deleteTagById(it.id!!) }
        val tag = bookService.save(TagDto(null, null, null, "my-tag-shelf"))
        Assertions.assertNotNull(tag.creationDate)
        Assertions.assertNotNull(tag.modificationDate)
        Assertions.assertNotNull(tag.id)
        Assertions.assertEquals("my-tag-shelf", tag.name)
        tags = bookService.findAllTags(null, Pageable.ofSize(100))
        Assertions.assertEquals(1, tags.totalElements)
        val saved = shelfService.save(CreateShelfDto(tag.name, tag.id!!), user())
        Assertions.assertEquals(tag.name, saved.name)
        Assertions.assertNotNull(saved.creationDate)
        Assertions.assertNotNull(saved.modificationDate)
        Assertions.assertNotNull(saved.id)
        Assertions.assertEquals(tag.id, saved.targetId)

        val userDto1 = userService.save(CreateUserDto(login = "testuser1", password = "1234", isAdmin = true))
        val user1 = userService.loadUserByUsername(userDto1.login)
        val saved1 = shelfService.save(CreateShelfDto(tag.name, tag.id!!), (user1 as JeluUser).user)
        Assertions.assertEquals(tag.name, saved1.name)
        Assertions.assertNotNull(saved1.creationDate)
        Assertions.assertNotNull(saved1.modificationDate)
        Assertions.assertNotNull(saved1.id)
        Assertions.assertEquals(tag.id, saved1.targetId)

        var shelves = shelfService.find(null, null, null)
        Assertions.assertEquals(2, shelves.size)

        bookService.deleteTagById(tag.id!!)
        tags = bookService.findAllTags(null, Pageable.ofSize(100))
        Assertions.assertEquals(0, tags.totalElements)
        shelves = shelfService.find(null, null, null)
        Assertions.assertEquals(0, shelves.size)
    }

    fun user(): UserDto {
        val userDetail = userService.loadUserByUsername("testuser")
        return (userDetail as JeluUser).user
    }
}
