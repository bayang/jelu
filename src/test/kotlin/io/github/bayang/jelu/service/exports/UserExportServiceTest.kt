package io.github.bayang.jelu.service.exports

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dto.CreateUserDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.dto.UserDto
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
import java.io.File

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserExportServiceTest(
    @Autowired private val userExportService: UserExportService,
    @Autowired private val userService: UserService,
    @Autowired private val jeluProperties: JeluProperties,
) {
    companion object {
        @TempDir
        lateinit var tempDir: File
    }

    @BeforeAll
    fun setupUser() {
        userService.save(CreateUserDto(login = "testuser", password = "1234", admin = true))
        jeluProperties.files.imports = tempDir.absolutePath
        println(jeluProperties.files.imports)
    }

    @AfterAll
    fun teardDown() {
        userService.findAll(null).forEach { userService.deleteUser(it.id!!) }
    }

    @AfterEach
    fun cleanTest() {
        userExportService.deleteAll()
    }

    @Test
    fun testInsertDelete() {
        val created = userExportService.save("file1.csv", user())
        Assertions.assertEquals("file1.csv", created.name)
        var nb = userExportService.existsByNameAndOwner(user(), "file1.csv")
        Assertions.assertEquals(1, nb)

        nb = userExportService.existsByNameAndOwner(user(), "file2.txt")
        Assertions.assertEquals(0, nb)
    }

    fun user(): UserDto {
        val userDetail = userService.loadUserByUsername("testuser")
        return (userDetail as JeluUser).user
    }
}
