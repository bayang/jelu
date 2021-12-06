package io.github.bayang.jelu.dto

import java.time.Instant
import java.util.*

data class UserBookDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val user: UserDto,
    val book: BookDto,
    val readingEvents: List<ReadingEventWithoutUserAndBookDto>?
)
data class UserBookLightDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
//    val user: UserDto,
    val book: BookDto,
    val readingEvents: List<ReadingEventWithoutUserAndBookDto>?
)
data class UserBookWithoutEventsDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val user: UserDto,
    val book: BookDto,
//    val readingEvents: List<ReadingEventWithoutUserAndBookDto>?
)