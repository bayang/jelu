package io.github.bayang.jelu.dto

import java.time.Instant
import java.util.*

data class UserDto(
    val id: UUID?,
    val creationDate: Instant?,
    val email: String,
    val password:String?,
    val modificationDate: Instant?,
    val isAdmin: Boolean,
)
data class UserDtoWithEvents(
    val id: UUID?,
    val creationDate: Instant?,
    val email: String,
    val password:String?,
    val modificationDate: Instant?,
    val isAdmin: Boolean,
    val readingEvents: List<ReadingEventWithoutUserDto>?
)
data class CreateUserDto(
    val email: String,
    val password:String,
    val isAdmin: Boolean,
)
