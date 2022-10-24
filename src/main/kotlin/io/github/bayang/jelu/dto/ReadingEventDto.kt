package io.github.bayang.jelu.dto

import io.github.bayang.jelu.dao.ReadingEventType
import java.time.Instant
import java.util.*

data class ReadingEventDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val eventType: ReadingEventType,
    val userBook: UserBookWithoutEventsDto,
    val startDate: Instant?,
    val endDate: Instant?,
)
data class ReadingEventWithoutUserBookDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val eventType: ReadingEventType,
    val startDate: Instant?,
    val endDate: Instant?,
)
data class CreateReadingEventWithUserInfoDto(
    val eventType: ReadingEventType,
    val bookId: UUID,
    val userId: UUID?
)
data class CreateReadingEventDto(
    val eventType: ReadingEventType,
    val bookId: UUID?,
    val eventDate: Instant?,
    val startDate: Instant?
)
data class UpdateReadingEventDto(
    val eventType: ReadingEventType,
    val eventDate: Instant?,
    val startDate: Instant?
)
