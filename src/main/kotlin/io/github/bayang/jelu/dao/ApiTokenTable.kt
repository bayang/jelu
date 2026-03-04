package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.ApiTokenDto
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.UUID

object ApiTokenTable : UUIDTable("api_token") {
    val userId = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val tokenHash = varchar("token_hash", 64).uniqueIndex()
    val name = varchar("name", 100)
    val scopes = varchar("scopes", 500)
    val createdAt = timestamp("created_at")
    val lastUsedAt = timestamp("last_used_at").nullable()
    val usageCount = long("usage_count").default(0)
    val expiresAt = timestamp("expires_at").nullable()
    val isActive = bool("is_active").default(true)
}

class ApiToken(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ApiToken>(ApiTokenTable)

    var user by User referencedOn ApiTokenTable.userId
    var tokenHash by ApiTokenTable.tokenHash
    var name by ApiTokenTable.name
    var scopes by ApiTokenTable.scopes
    var createdAt by ApiTokenTable.createdAt
    var lastUsedAt by ApiTokenTable.lastUsedAt
    var usageCount by ApiTokenTable.usageCount
    var expiresAt by ApiTokenTable.expiresAt
    var isActive by ApiTokenTable.isActive

    fun toApiTokenDto(): ApiTokenDto =
        ApiTokenDto(
            id = this.id.value,
            userId = this.user.id.value,
            name = this.name,
            scopes = this.scopes.split(",").filter { it.isNotBlank() },
            createdAt = this.createdAt,
            lastUsedAt = this.lastUsedAt,
            usageCount = this.usageCount,
            expiresAt = this.expiresAt,
            isActive = this.isActive,
        )
}
