package io.github.bayang.jelu.dto

import io.github.bayang.jelu.dao.ReadingEventType
import java.time.Instant
import java.util.UUID

data class UserBookDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val user: UserDto,
    val book: BookDto,
    val readingEvents: List<ReadingEventWithoutUserBookDto>?,
    val lastReadingEventDate: Instant?,
    val lastReadingEvent: ReadingEventType?,
    val personalNotes: String?,
    val owned: Boolean?,
    val toRead: Boolean?,
    val percentRead: Int?,
    val currentPageNumber: Int?,
    val borrowed: Boolean?,
)
data class UserBookLightDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val book: BookDto,
    val readingEvents: List<ReadingEventWithoutUserBookDto>?,
    val lastReadingEventDate: Instant?,
    val lastReadingEvent: ReadingEventType?,
    val personalNotes: String?,
    val owned: Boolean?,
    val toRead: Boolean?,
    val percentRead: Int?,
    val currentPageNumber: Int?,
    val borrowed: Boolean?,
)
data class UserBookLightWithoutBookDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val readingEvents: List<ReadingEventWithoutUserBookDto>?,
    val lastReadingEventDate: Instant?,
    val lastReadingEvent: ReadingEventType?,
    val personalNotes: String?,
    val owned: Boolean?,
    val toRead: Boolean?,
    val percentRead: Int?,
    val currentPageNumber: Int?,
    val borrowed: Boolean?,
)
data class UserBookWithoutEventsAndUserDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val book: BookDto,
    val lastReadingEventDate: Instant?,
    val lastReadingEvent: ReadingEventType?,
    val personalNotes: String?,
    val owned: Boolean?,
    val toRead: Boolean?,
    val percentRead: Int?,
    val currentPageNumber: Int?,
    val borrowed: Boolean?,
    val avgRating: Double? = null,
    val userAvgRating: Double? = null,
)
data class UserBookWithoutEventsDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val user: UserDto,
    val book: BookDto,
    val personalNotes: String?,
    val owned: Boolean?,
    val toRead: Boolean?,
    val percentRead: Int?,
    val currentPageNumber: Int?,
    val borrowed: Boolean?,
)
data class CreateUserBookDto(
    val lastReadingEvent: ReadingEventType?,
    val lastReadingEventDate: Instant?,
    val personalNotes: String?,
    val owned: Boolean?,
    val book: BookCreateDto,
    val toRead: Boolean?,
    val percentRead: Int?,
    val currentPageNumber: Int?,
    val borrowed: Boolean?,
)
data class UserBookUpdateDto(
    val lastReadingEvent: ReadingEventType?,
    val personalNotes: String?,
    val owned: Boolean?,
    val book: BookCreateDto?,
    val toRead: Boolean?,
    val percentRead: Int?,
    val currentPageNumber: Int?,
    val borrowed: Boolean?,
)
data class UserBookBulkUpdateDto(
    val ids: List<UUID>,
    val toRead: Boolean?,
    val owned: Boolean?,
    val removeTags: List<UUID>?,
    val addTags: List<UUID>?,
)
