package io.github.bayang.jelu.dto

import io.github.bayang.jelu.dao.Provider
import java.time.Instant
import java.util.UUID

data class UserDto(
    val id: UUID?,
    val creationDate: Instant?,
    val login: String,
    val password: String?,
    val modificationDate: Instant?,
    val isAdmin: Boolean,
    val provider: Provider = Provider.JELU_DB,
)
data class CreateUserDto(
    val login: String,
    val password: String,
    val isAdmin: Boolean,
    val provider: Provider = Provider.JELU_DB,
)
data class UpdateUserDto(
    val password: String,
    val isAdmin: Boolean?,
    val provider: Provider?,
)
data class AuthenticationDto(
    val user: UserDto,
    val token: String?,
)
