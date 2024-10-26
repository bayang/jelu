package io.github.bayang.jelu.service

import io.github.bayang.jelu.dao.MessageCategory
import io.github.bayang.jelu.dto.CreateUserDto
import io.github.bayang.jelu.dto.CreateUserMessageDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.dto.UpdateUserMessageDto
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
class UserMessageServiceTest(
    @Autowired private val userMessageService: UserMessageService,
    @Autowired private val userService: UserService,
) {

    @BeforeAll
    fun setupUser() {
        userService.save(CreateUserDto(login = "testuser", password = "1234", isAdmin = true))
    }

    @AfterAll
    fun teardDown() {
        userMessageService.find(user(), null, null, Pageable.ofSize(200))
            .forEach { userMessageDto -> userMessageService.delete(userMessageDto.id!!) }
        userService.findAll(null).forEach { userService.deleteUser(it.id!!) }
    }

    @Test
    fun testSaveFindAndUpdate() {
        val saved = userMessageService.save(
            CreateUserMessageDto(
                "this is a message",
                "/test/myfile.csv",
                MessageCategory.INFO,
            ),
            user(),
        )
        Assertions.assertEquals("this is a message", saved.message)
        Assertions.assertEquals(MessageCategory.INFO, saved.category)
        Assertions.assertNotNull(saved.creationDate)
        Assertions.assertNotNull(saved.modificationDate)
        Assertions.assertNotNull(saved.id)
        Assertions.assertEquals("/test/myfile.csv", saved.link)

        val found = userMessageService.find(user(), null, null, Pageable.ofSize(30))
        Assertions.assertEquals(1, found.totalElements)
        val first = found.content[0]
        Assertions.assertEquals("this is a message", first.message)
        Assertions.assertEquals(MessageCategory.INFO, first.category)
        Assertions.assertNotNull(first.creationDate)
        Assertions.assertNotNull(first.modificationDate)
        Assertions.assertNotNull(saved.id)
        Assertions.assertEquals("/test/myfile.csv", first.link)

        val readMessage = userMessageService.update(first.id!!, UpdateUserMessageDto(null, null, null, true))
        Assertions.assertEquals(true, readMessage.read)
        Assertions.assertEquals("this is a message", readMessage.message)
        Assertions.assertEquals(MessageCategory.INFO, readMessage.category)
        Assertions.assertNotNull(readMessage.creationDate)
        Assertions.assertNotNull(readMessage.modificationDate)
        Assertions.assertNotNull(readMessage.id)
        Assertions.assertEquals("/test/myfile.csv", readMessage.link)

        val notFound = userMessageService.find(user(), false, null, Pageable.ofSize(30))
        Assertions.assertEquals(0, notFound.totalElements)

        val foundRead = userMessageService.find(user(), true, null, Pageable.ofSize(30))
        Assertions.assertEquals(1, foundRead.totalElements)

        val foundCategory = userMessageService.find(user(), null, listOf(MessageCategory.INFO), Pageable.ofSize(30))
        Assertions.assertEquals(1, foundCategory.totalElements)

        val notFoundCategory = userMessageService.find(user(), null, listOf(MessageCategory.ERROR), Pageable.ofSize(30))
        Assertions.assertEquals(0, notFoundCategory.totalElements)

        val foundSeveralCategories = userMessageService.find(user(), null, listOf(MessageCategory.ERROR, MessageCategory.INFO), Pageable.ofSize(30))
        Assertions.assertEquals(1, foundSeveralCategories.totalElements)
    }

    fun user(): UserDto {
        val userDetail = userService.loadUserByUsername("testuser")
        return (userDetail as JeluUser).user
    }
}
