package io.github.bayang.jelu.dto

import io.github.bayang.jelu.security.TokenScope
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

/**
 * DTO for API token responses (does not include the token hash)
 */
data class ApiTokenDto(
    val id: UUID,
    val userId: UUID,
    val name: String,
    val scopes: List<String>,
    val createdAt: Instant,
    val lastUsedAt: Instant?,
    val usageCount: Long,
    val expiresAt: Instant?,
    val isActive: Boolean,
)

/**
 * DTO for creating a new API token
 */
data class CreateApiTokenDto(
    @field:NotBlank(message = "Token name is required")
    @field:Size(max = 100, message = "Token name must be at most 100 characters")
    val name: String,
    @field:Size(min = 1, message = "At least one scope is required")
    val scopes: List<String>,
    val expiresAt: Instant? = null,
)

/**
 * DTO for updating an existing API token
 */
data class UpdateApiTokenDto(
    @field:Size(max = 100, message = "Token name must be at most 100 characters")
    val name: String? = null,
    val scopes: List<String>? = null,
    val isActive: Boolean? = null,
)

/**
 * Response DTO that includes the raw token (only returned once on creation)
 */
data class ApiTokenCreatedDto(
    val token: ApiTokenDto,
    val rawToken: String,
)

/**
 * DTO for listing available scopes
 */
data class TokenScopeDto(
    val name: String,
    val description: String,
    val category: String,
) {
    companion object {
        fun fromTokenScope(scope: TokenScope): TokenScopeDto =
            TokenScopeDto(
                name = scope.scopeName,
                description = scope.description,
                category = scope.category.displayName,
            )

        fun allScopes(): List<TokenScopeDto> = TokenScope.entries.map { fromTokenScope(it) }
    }
}

/**
 * DTO for admin view of tokens (includes user info)
 */
data class AdminApiTokenDto(
    val id: UUID,
    val userId: UUID,
    val userLogin: String,
    val name: String,
    val scopes: List<String>,
    val createdAt: Instant,
    val lastUsedAt: Instant?,
    val usageCount: Long,
    val expiresAt: Instant?,
    val isActive: Boolean,
)

/**
 * DTO for validated API token (includes full user data for authentication)
 */
data class ValidatedApiTokenDto(
    val id: UUID,
    val name: String,
    val scopes: List<String>,
    val user: UserDto,
)
