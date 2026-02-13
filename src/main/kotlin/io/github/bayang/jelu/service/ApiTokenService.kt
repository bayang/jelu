package io.github.bayang.jelu.service

import io.github.bayang.jelu.dao.ApiToken
import io.github.bayang.jelu.dao.ApiTokenRepository
import io.github.bayang.jelu.dto.AdminApiTokenDto
import io.github.bayang.jelu.dto.ApiTokenCreatedDto
import io.github.bayang.jelu.dto.ApiTokenDto
import io.github.bayang.jelu.dto.CreateApiTokenDto
import io.github.bayang.jelu.dto.UpdateApiTokenDto
import io.github.bayang.jelu.dto.ValidatedApiTokenDto
import io.github.bayang.jelu.errors.JeluValidationException
import io.github.bayang.jelu.security.TokenScope
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

const val TOKEN_PREFIX = "jelu_"
const val TOKEN_HEX_LENGTH = 32

@Component
class ApiTokenService(
    private val apiTokenRepository: ApiTokenRepository,
) {
    private val secureRandom = SecureRandom()

    /**
     * Generate a new API token with the format: jelu_<32-hex-chars>
     */
    fun generateRawToken(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        val hex = bytes.joinToString("") { "%02x".format(it) }
        return "$TOKEN_PREFIX$hex"
    }

    /**
     * Hash a token using SHA-256
     */
    fun hashToken(rawToken: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(rawToken.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Validate that a raw token has the correct format
     */
    fun isValidTokenFormat(rawToken: String): Boolean {
        if (!rawToken.startsWith(TOKEN_PREFIX)) return false
        val hexPart = rawToken.removePrefix(TOKEN_PREFIX)
        if (hexPart.length != TOKEN_HEX_LENGTH) return false
        return hexPart.all { it in '0'..'9' || it in 'a'..'f' }
    }

    @Transactional
    fun createToken(
        createDto: CreateApiTokenDto,
        userId: UUID,
    ): ApiTokenCreatedDto {
        // Validate scopes
        if (!TokenScope.validateScopes(createDto.scopes)) {
            val invalidScopes = createDto.scopes.filter { !TokenScope.isValidScope(it) }
            throw JeluValidationException("Invalid scopes: $invalidScopes")
        }

        // Validate expiration date if provided
        createDto.expiresAt?.let {
            if (it.isBefore(Instant.now())) {
                throw JeluValidationException("Expiration date must be in the future")
            }
        }

        // Check if user already has too many tokens (limit to 20)
        val existingTokens = apiTokenRepository.findByUserId(userId)
        if (existingTokens.size >= 20) {
            throw JeluValidationException("Maximum number of API tokens (20) reached")
        }

        // Generate and hash token
        val rawToken = generateRawToken()
        val tokenHash = hashToken(rawToken)

        // Save token
        val savedToken = apiTokenRepository.save(createDto, tokenHash, userId)

        logger.info { "Created API token '${createDto.name}' for user $userId" }

        return ApiTokenCreatedDto(
            token = savedToken.toApiTokenDto(),
            rawToken = rawToken,
        )
    }

    @Transactional
    fun findByUserId(userId: UUID): List<ApiTokenDto> = apiTokenRepository.findByUserId(userId).map { it.toApiTokenDto() }

    @Transactional
    fun findAll(): List<AdminApiTokenDto> = apiTokenRepository.findAll().map { it.toAdminApiTokenDto() }

    @Transactional
    fun findById(id: UUID): ApiTokenDto? = apiTokenRepository.findById(id)?.toApiTokenDto()

    @Transactional
    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): ApiTokenDto? {
        val token = apiTokenRepository.findById(id)
        return if (token != null && token.user.id.value == userId) {
            token.toApiTokenDto()
        } else {
            null
        }
    }

    @Transactional
    fun updateToken(
        id: UUID,
        userId: UUID,
        updateDto: UpdateApiTokenDto,
        isAdmin: Boolean = false,
    ): ApiTokenDto? {
        val token = apiTokenRepository.findById(id) ?: return null

        // Non-admin users can only update their own tokens
        if (!isAdmin && token.user.id.value != userId) {
            throw JeluValidationException("You can only update your own tokens")
        }

        // Validate scopes if provided
        updateDto.scopes?.let {
            if (!TokenScope.validateScopes(it)) {
                val invalidScopes = it.filter { scope -> !TokenScope.isValidScope(scope) }
                throw JeluValidationException("Invalid scopes: $invalidScopes")
            }
        }

        return apiTokenRepository.update(id, updateDto)?.toApiTokenDto()
    }

    @Transactional
    fun revokeToken(
        id: UUID,
        userId: UUID,
        isAdmin: Boolean = false,
    ): Boolean {
        val token = apiTokenRepository.findById(id) ?: return false

        // Non-admin users can only revoke their own tokens
        if (!isAdmin && token.user.id.value != userId) {
            throw JeluValidationException("You can only revoke your own tokens")
        }

        logger.info { "Revoking API token '${token.name}' (id: $id)" }
        return apiTokenRepository.delete(id)
    }

    /**
     * Validate a raw token and return token details with user info if valid.
     * All data is loaded within the transaction to avoid lazy loading issues.
     */
    @Transactional
    fun validateToken(rawToken: String): ValidatedApiTokenDto? {
        if (!isValidTokenFormat(rawToken)) {
            logger.debug { "Invalid token format" }
            return null
        }

        val tokenHash = hashToken(rawToken)
        val token = apiTokenRepository.findByTokenHash(tokenHash)

        if (token == null) {
            logger.debug { "Token not found" }
            return null
        }

        if (!token.isActive) {
            logger.debug { "Token is inactive" }
            return null
        }

        token.expiresAt?.let {
            if (it.isBefore(Instant.now())) {
                logger.debug { "Token has expired" }
                return null
            }
        }

        // Load all required data within the transaction
        return ValidatedApiTokenDto(
            id = token.id.value,
            name = token.name,
            scopes = token.scopes.split(",").filter { it.isNotBlank() },
            user = token.user.toUserDto(),
        )
    }

    /**
     * Update last used timestamp asynchronously
     */
    @Async
    @Transactional
    fun updateLastUsedAsync(tokenId: UUID) {
        try {
            apiTokenRepository.updateLastUsed(tokenId)
        } catch (e: Exception) {
            logger.warn { "Failed to update last used timestamp for token $tokenId: ${e.message}" }
        }
    }

    private fun ApiToken.toAdminApiTokenDto(): AdminApiTokenDto =
        AdminApiTokenDto(
            id = this.id.value,
            userId = this.user.id.value,
            userLogin = this.user.login,
            name = this.name,
            scopes = this.scopes.split(",").filter { it.isNotBlank() },
            createdAt = this.createdAt,
            lastUsedAt = this.lastUsedAt,
            usageCount = this.usageCount,
            expiresAt = this.expiresAt,
            isActive = this.isActive,
        )
}
