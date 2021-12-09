package io.github.bayang.jelu.dto

import io.github.bayang.jelu.dao.ReadingEventType
import java.time.Instant
import java.util.*

data class UserBookDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val user: UserDto,
    val book: BookDto,
    val readingEvents: List<ReadingEventWithoutUserBookDto>?,
    val lastReadingEvent: ReadingEventType,
    val personalNotes: String?,
    val owned: Boolean?
)
data class UserBookLightDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val book: BookDto,
    val readingEvents: List<ReadingEventWithoutUserBookDto>?,
    val lastReadingEvent: ReadingEventType,
    val personalNotes: String?,
    val owned: Boolean?
)
data class UserBookWithoutEventsDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val user: UserDto,
    val book: BookDto,
    val personalNotes: String?,
    val owned: Boolean?
)
data class CreateUserBookDto(
    val lastReadingEvent: ReadingEventType?,
    val personalNotes: String?,
    val owned: Boolean?,
    val book: BookCreateDto
)
data class UserBookUpdateDto(
    val lastReadingEvent: ReadingEventType?,
    val personalNotes: String?,
    val owned: Boolean?,
    val book: BookCreateDto?
)