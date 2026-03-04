package io.github.bayang.jelu.service

import io.github.bayang.jelu.dto.CreateApiTokenDto
import io.github.bayang.jelu.dto.CreateUserDto
import io.github.bayang.jelu.dto.UpdateApiTokenDto
import io.github.bayang.jelu.dto.UserDto
import io.github.bayang.jelu.errors.JeluValidationException
import io.github.bayang.jelu.security.TokenScope
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant
import java.time.temporal.ChronoUnit

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiTokenServiceTest(
    @Autowired private val apiTokenService: ApiTokenService,
    @Autowired private val userService: UserService,
) {
    private lateinit var testUser: UserDto

    @BeforeAll
    fun setupUser() {
        testUser = userService.save(CreateUserDto(login = "apitokenuser", password = "1234", isAdmin = true))
    }

    @AfterAll
    fun tearDown() {
        // Clean up tokens first (they have FK to user)
        apiTokenService.findByUserId(testUser.id!!).forEach {
            apiTokenService.revokeToken(it.id, testUser.id!!, isAdmin = true)
        }
        // Then clean up user
        userService.findAll(null).filter { it.login == "apitokenuser" }.forEach {
            userService.deleteUser(it.id!!)
        }
    }

    @Test
    fun testTokenGeneration() {
        val rawToken = apiTokenService.generateRawToken()
        Assertions.assertTrue(rawToken.startsWith("jelu_"))
        Assertions.assertEquals(37, rawToken.length) // "jelu_" (5) + 32 hex chars
        Assertions.assertTrue(apiTokenService.isValidTokenFormat(rawToken))
    }

    @Test
    fun testTokenHashing() {
        val rawToken = "jelu_0123456789abcdef0123456789abcdef"
        val hash1 = apiTokenService.hashToken(rawToken)
        val hash2 = apiTokenService.hashToken(rawToken)

        // Same input should produce same hash
        Assertions.assertEquals(hash1, hash2)
        // Hash should be 64 chars (SHA-256 hex)
        Assertions.assertEquals(64, hash1.length)
        // Hash should be different from input
        Assertions.assertNotEquals(rawToken, hash1)
    }

    @Test
    fun testInvalidTokenFormats() {
        Assertions.assertFalse(apiTokenService.isValidTokenFormat("invalid"))
        Assertions.assertFalse(apiTokenService.isValidTokenFormat("jelu_tooshort"))
        Assertions.assertFalse(apiTokenService.isValidTokenFormat("jelu_INVALID_HEX_CHARACTERS_HERE!!"))
        Assertions.assertFalse(apiTokenService.isValidTokenFormat("other_0123456789abcdef0123456789abcdef"))
    }

    @Test
    fun testCreateAndFindToken() {
        // Clean up any existing tokens
        apiTokenService.findByUserId(testUser.id!!).forEach {
            apiTokenService.revokeToken(it.id, testUser.id!!, isAdmin = true)
        }

        val createDto =
            CreateApiTokenDto(
                name = "Test Token",
                scopes = listOf(TokenScope.BOOKS_READ.scopeName, TokenScope.BOOKS_WRITE.scopeName),
                expiresAt = Instant.now().plus(30, ChronoUnit.DAYS),
            )

        val createdResult = apiTokenService.createToken(createDto, testUser.id!!)

        // Verify the raw token is returned
        Assertions.assertNotNull(createdResult.rawToken)
        Assertions.assertTrue(createdResult.rawToken.startsWith("jelu_"))
        Assertions.assertTrue(apiTokenService.isValidTokenFormat(createdResult.rawToken))

        // Verify the token DTO
        Assertions.assertEquals("Test Token", createdResult.token.name)
        Assertions.assertEquals(testUser.id, createdResult.token.userId)
        Assertions.assertTrue(createdResult.token.scopes.contains(TokenScope.BOOKS_READ.scopeName))
        Assertions.assertTrue(createdResult.token.scopes.contains(TokenScope.BOOKS_WRITE.scopeName))
        Assertions.assertTrue(createdResult.token.isActive)
        Assertions.assertEquals(0, createdResult.token.usageCount)
        Assertions.assertNull(createdResult.token.lastUsedAt)

        // Find by user ID
        val tokens = apiTokenService.findByUserId(testUser.id!!)
        Assertions.assertEquals(1, tokens.size)
        Assertions.assertEquals("Test Token", tokens[0].name)

        // Find by ID
        val foundToken = apiTokenService.findById(createdResult.token.id)
        Assertions.assertNotNull(foundToken)
        Assertions.assertEquals("Test Token", foundToken!!.name)
    }

    @Test
    fun testValidateToken() {
        // Clean up any existing tokens
        apiTokenService.findByUserId(testUser.id!!).forEach {
            apiTokenService.revokeToken(it.id, testUser.id!!, isAdmin = true)
        }

        val createDto =
            CreateApiTokenDto(
                name = "Validation Test Token",
                scopes = listOf(TokenScope.BOOKS_READ.scopeName),
            )

        val createdResult = apiTokenService.createToken(createDto, testUser.id!!)

        // Valid token should be validated
        val validatedToken = apiTokenService.validateToken(createdResult.rawToken)
        Assertions.assertNotNull(validatedToken)
        Assertions.assertEquals("Validation Test Token", validatedToken!!.name)

        // Invalid token should return null
        val invalidToken = apiTokenService.validateToken("jelu_0000000000000000000000000000000000")
        Assertions.assertNull(invalidToken)
    }

    @Test
    fun testUpdateToken() {
        // Clean up any existing tokens
        apiTokenService.findByUserId(testUser.id!!).forEach {
            apiTokenService.revokeToken(it.id, testUser.id!!, isAdmin = true)
        }

        val createDto =
            CreateApiTokenDto(
                name = "Original Name",
                scopes = listOf(TokenScope.BOOKS_READ.scopeName),
            )

        val createdResult = apiTokenService.createToken(createDto, testUser.id!!)

        // Update the token
        val updateDto =
            UpdateApiTokenDto(
                name = "Updated Name",
                scopes = listOf(TokenScope.BOOKS_READ.scopeName, TokenScope.BOOKS_WRITE.scopeName),
                isActive = true,
            )

        val updatedToken = apiTokenService.updateToken(createdResult.token.id, testUser.id!!, updateDto)
        Assertions.assertNotNull(updatedToken)
        Assertions.assertEquals("Updated Name", updatedToken!!.name)
        Assertions.assertEquals(2, updatedToken.scopes.size)
    }

    @Test
    fun testDeactivateToken() {
        // Clean up any existing tokens
        apiTokenService.findByUserId(testUser.id!!).forEach {
            apiTokenService.revokeToken(it.id, testUser.id!!, isAdmin = true)
        }

        val createDto =
            CreateApiTokenDto(
                name = "Deactivate Test Token",
                scopes = listOf(TokenScope.BOOKS_READ.scopeName),
            )

        val createdResult = apiTokenService.createToken(createDto, testUser.id!!)
        val rawToken = createdResult.rawToken

        // Token should be valid initially
        Assertions.assertNotNull(apiTokenService.validateToken(rawToken))

        // Deactivate the token
        val updateDto = UpdateApiTokenDto(isActive = false)
        apiTokenService.updateToken(createdResult.token.id, testUser.id!!, updateDto)

        // Token should no longer be valid
        Assertions.assertNull(apiTokenService.validateToken(rawToken))
    }

    @Test
    fun testRevokeToken() {
        // Clean up any existing tokens
        apiTokenService.findByUserId(testUser.id!!).forEach {
            apiTokenService.revokeToken(it.id, testUser.id!!, isAdmin = true)
        }

        val createDto =
            CreateApiTokenDto(
                name = "Revoke Test Token",
                scopes = listOf(TokenScope.BOOKS_READ.scopeName),
            )

        val createdResult = apiTokenService.createToken(createDto, testUser.id!!)

        // Token should exist
        Assertions.assertNotNull(apiTokenService.findById(createdResult.token.id))

        // Revoke the token
        val revoked = apiTokenService.revokeToken(createdResult.token.id, testUser.id!!)
        Assertions.assertTrue(revoked)

        // Token should no longer exist
        Assertions.assertNull(apiTokenService.findById(createdResult.token.id))

        // Revoking again should return false
        val revokedAgain = apiTokenService.revokeToken(createdResult.token.id, testUser.id!!)
        Assertions.assertFalse(revokedAgain)
    }

    @Test
    fun testInvalidScopes() {
        val createDto =
            CreateApiTokenDto(
                name = "Invalid Scope Token",
                scopes = listOf("invalid:scope"),
            )

        assertThrows<JeluValidationException> {
            apiTokenService.createToken(createDto, testUser.id!!)
        }
    }

    @Test
    fun testExpiredToken() {
        // Clean up any existing tokens
        apiTokenService.findByUserId(testUser.id!!).forEach {
            apiTokenService.revokeToken(it.id, testUser.id!!, isAdmin = true)
        }

        // Create token that expires in the past (should fail)
        val pastExpiration = Instant.now().minus(1, ChronoUnit.DAYS)
        val createDto =
            CreateApiTokenDto(
                name = "Expired Token",
                scopes = listOf(TokenScope.BOOKS_READ.scopeName),
                expiresAt = pastExpiration,
            )

        assertThrows<JeluValidationException> {
            apiTokenService.createToken(createDto, testUser.id!!)
        }
    }

    @Test
    fun testMaxTokenLimit() {
        // Clean up any existing tokens
        apiTokenService.findByUserId(testUser.id!!).forEach {
            apiTokenService.revokeToken(it.id, testUser.id!!, isAdmin = true)
        }

        // Create 20 tokens (the maximum)
        for (i in 1..20) {
            val createDto =
                CreateApiTokenDto(
                    name = "Token $i",
                    scopes = listOf(TokenScope.BOOKS_READ.scopeName),
                )
            apiTokenService.createToken(createDto, testUser.id!!)
        }

        // 21st token should fail
        val createDto =
            CreateApiTokenDto(
                name = "Token 21",
                scopes = listOf(TokenScope.BOOKS_READ.scopeName),
            )

        assertThrows<JeluValidationException> {
            apiTokenService.createToken(createDto, testUser.id!!)
        }
    }
}
