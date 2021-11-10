package io.github.bayang.jelu.dto

import io.github.bayang.jelu.dao.ReadingEventType
import java.time.Instant
import java.util.*

data class ReadingEventDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val eventType: ReadingEventType,
    val book: BookDto,
    val user: UserDto
)
data class ReadingEventWithoutUserDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val eventType: ReadingEventType,
    val book: BookDto,
)
data class CreateReadingEventDto(
    val eventType: ReadingEventType,
    val bookId:UUID,
    //FIXME backend should get current logged user
    val userId: UUID
)
data class UpdateReadingEventDto(
    val eventType: ReadingEventType,
)