package io.github.bayang.jelu.service

import io.github.bayang.jelu.dto.CreateUserDto
import io.github.bayang.jelu.errors.JeluException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class UserServiceTest(@Autowired private val userService: UserService) {

    @BeforeEach
    fun emptyTable() {
        userService.findAll(null).forEach { it.id?.let { it1 -> userService.deleteUser(it1) } }
    }

    @Test
    fun testFirstUserLifecycle() {
        Assertions.assertTrue(userService.isInitialSetup())

        val noUser = userService.loadUserByUsername("login1")
        Assertions.assertEquals("setup", noUser.username)

        val created = userService.save(CreateUserDto(login = "login1", password = "password", isAdmin = true))
        Assertions.assertEquals("login1", created.login)
        Assertions.assertTrue(created.isAdmin)
        assertThrows<JeluException> { userService.save(CreateUserDto(login = "login1", password = "password2", isAdmin = true)) }
        Assertions.assertFalse(userService.isInitialSetup())

        val found = userService.findByLogin("login1")
        Assertions.assertEquals("login1", found[0].login)
    }
}
