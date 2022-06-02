package io.github.bayang.jelu.service

import io.github.bayang.jelu.dao.Provider
import io.github.bayang.jelu.dto.CreateUserDto
import io.github.bayang.jelu.dto.UpdateUserDto
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
        Assertions.assertEquals(Provider.JELU_DB, created.provider)
        assertThrows<JeluException> { userService.save(CreateUserDto(login = "login1", password = "password2", isAdmin = true)) }
        Assertions.assertFalse(userService.isInitialSetup())

        var found = userService.findByLogin("login1")
        Assertions.assertEquals("login1", found[0].login)

        Assertions.assertEquals(1, userService.findByLoginAndProvider(created.login, Provider.JELU_DB).size)
        Assertions.assertEquals(0, userService.findByLoginAndProvider(created.login, Provider.LDAP).size)

        val updated = userService.updateUser(created.id!!, UpdateUserDto(isAdmin = false, password = "newpass", provider = null))
        Assertions.assertEquals(false, updated.isAdmin)

        found = userService.findByLogin("login1")
        Assertions.assertEquals(false, found[0].isAdmin)

        val createdLdap = userService.save(CreateUserDto(login = "loginldap", password = "password", isAdmin = true, Provider.LDAP))
        Assertions.assertEquals("loginldap", createdLdap.login)
        Assertions.assertTrue(createdLdap.isAdmin)
        Assertions.assertEquals(Provider.LDAP, createdLdap.provider)

        Assertions.assertEquals(1, userService.findByLoginAndProvider(createdLdap.login, Provider.LDAP).size)
        Assertions.assertEquals(0, userService.findByLoginAndProvider(createdLdap.login, Provider.JELU_DB).size)
    }
}
