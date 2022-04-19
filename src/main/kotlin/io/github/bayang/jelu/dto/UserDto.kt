package io.github.bayang.jelu.dto

import java.time.Instant
import java.util.*

data class UserDto(
    val id: UUID?,
    val creationDate: Instant?,
    val login: String,
    val password: String?,
    val modificationDate: Instant?,
    val isAdmin: Boolean,
)
data class CreateUserDto(
    val login: String,
    val password: String,
    val isAdmin: Boolean,
)
data class UpdateUserDto(
    val password: String,
    val isAdmin: Boolean?,
)
data class AuthenticationDto(
    val user: UserDto,
    val token: String?
)
