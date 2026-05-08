package io.github.bayang.jelu.dto

import io.github.bayang.jelu.dao.ReadingEventType
import java.time.OffsetDateTime
import java.util.UUID

data class ReadingEventDto(
    val id: UUID?,
    val creationDate: OffsetDateTime?,
    val modificationDate: OffsetDateTime?,
    val eventType: ReadingEventType,
    val userBook: UserBookWithoutEventsDto,
    val startDate: OffsetDateTime?,
    val endDate: OffsetDateTime?,
)

data class ReadingEventStatsDto(
    val id: UUID?,
    val creationDate: OffsetDateTime?,
    val modificationDate: OffsetDateTime?,
    val eventType: ReadingEventType,
    val priceInCents: Long?,
    val startDate: OffsetDateTime?,
    val userBook: UserBookWithoutEventsDto,
    val endDate: OffsetDateTime?,
)

data class ReadingEventWithoutUserBookDto(
    val id: UUID?,
    val creationDate: OffsetDateTime?,
    val modificationDate: OffsetDateTime?,
    val eventType: ReadingEventType,
    val startDate: OffsetDateTime?,
    val endDate: OffsetDateTime?,
)

data class CreateReadingEventWithUserInfoDto(
    val eventType: ReadingEventType,
    val bookId: UUID,
    val userId: UUID?,
)

data class CreateReadingEventDto(
    val eventType: ReadingEventType,
    val bookId: UUID?,
    val eventDate: OffsetDateTime?,
    val startDate: OffsetDateTime?,
)

data class UpdateReadingEventDto(
    val eventType: ReadingEventType,
    val eventDate: OffsetDateTime?,
    val startDate: OffsetDateTime?,
)

enum class ReadingEventTypeFilter {
    FINISHED,
    DROPPED,
    CURRENTLY_READING,
    NONE,
}
