package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.CreateApiTokenDto
import io.github.bayang.jelu.dto.UpdateApiTokenDto
import io.github.bayang.jelu.utils.nowInstant
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Repository
class ApiTokenRepository {
    fun save(
        createApiTokenDto: CreateApiTokenDto,
        tokenHash: String,
        userId: UUID,
    ): ApiToken {
        val instant: Instant = nowInstant()
        return ApiToken.new {
            this.user = User[userId]
            this.tokenHash = tokenHash
            this.name = createApiTokenDto.name
            this.scopes = createApiTokenDto.scopes.joinToString(",")
            this.createdAt = instant
            this.expiresAt = createApiTokenDto.expiresAt
            this.isActive = true
            this.usageCount = 0
        }
    }

    fun findByTokenHash(tokenHash: String): ApiToken? {
        val query =
            ApiTokenTable
                .selectAll()
                .andWhere { ApiTokenTable.tokenHash eq tokenHash }
        return ApiToken.wrapRows(query).firstOrNull()
    }

    fun findByUserId(userId: UUID): List<ApiToken> {
        val query =
            ApiTokenTable
                .selectAll()
                .andWhere { ApiTokenTable.userId eq userId }
        return ApiToken.wrapRows(query).toList()
    }

    fun findAll(): List<ApiToken> = ApiToken.all().toList()

    fun findById(id: UUID): ApiToken? = ApiToken.findById(id)

    fun update(
        id: UUID,
        updateDto: UpdateApiTokenDto,
    ): ApiToken? {
        val token = ApiToken.findById(id) ?: return null
        updateDto.name?.let { token.name = it }
        updateDto.scopes?.let { token.scopes = it.joinToString(",") }
        updateDto.isActive?.let { token.isActive = it }
        return token
    }

    fun updateLastUsed(id: UUID) {
        val token = ApiToken.findById(id)
        token?.let {
            it.lastUsedAt = nowInstant()
            it.usageCount = it.usageCount + 1
        }
    }

    fun delete(id: UUID): Boolean {
        val token = ApiToken.findById(id)
        return if (token != null) {
            token.delete()
            true
        } else {
            false
        }
    }

    fun deleteByUserId(userId: UUID): Int {
        val tokens = findByUserId(userId)
        tokens.forEach { it.delete() }
        return tokens.size
    }
}
