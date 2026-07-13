package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.ReadingEventDto
import io.github.bayang.jelu.dto.ReadingEventStatsDto
import io.github.bayang.jelu.dto.ReadingEventWithoutUserBookDto
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import java.time.OffsetDateTime
import java.util.UUID

object ReadingEventTable : UUIDTable("reading_event") {
    val creationDate = timestampWithTimeZone("creation_date")
    val modificationDate = timestampWithTimeZone("modification_date")
    val userBook = reference("user_book", UserBookTable, onDelete = ReferenceOption.CASCADE)
    val eventType = enumerationByName("event_type", 200, ReadingEventType::class)
    val startDate = timestampWithTimeZone("start_date")
    val endDate = timestampWithTimeZone("end_date").nullable()
}

class ReadingEvent(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ReadingEvent>(ReadingEventTable)

    var creationDate by ReadingEventTable.creationDate
    var modificationDate by ReadingEventTable.modificationDate
    var userBook by UserBook referencedOn ReadingEventTable.userBook
    var eventType by ReadingEventTable.eventType
    var startDate by ReadingEventTable.startDate
    var endDate by ReadingEventTable.endDate
    val lastEventDate: OffsetDateTime
        get() {
            endDate?.let { return it }
            return startDate
        }

    fun toReadingEventDto(): ReadingEventDto =
        ReadingEventDto(
            id = this.id.value,
            creationDate = this.creationDate,
            modificationDate = this.modificationDate,
            userBook = this.userBook.toUserBookWithoutEventsDto(),
            eventType = this.eventType,
            startDate = this.startDate,
            endDate = this.endDate,
        )

    fun toReadingEventStatsDto(): ReadingEventStatsDto =
        ReadingEventStatsDto(
            id = this.id.value,
            creationDate = this.creationDate,
            modificationDate = this.modificationDate,
            priceInCents = this.userBook.priceInCents,
            eventType = this.eventType,
            startDate = this.startDate,
            userBook = this.userBook.toUserBookWithoutEventsDto(),
            endDate = this.endDate,
        )

    fun toReadingEventWithoutUserBookDto(): ReadingEventWithoutUserBookDto =
        ReadingEventWithoutUserBookDto(
            id = this.id.value,
            creationDate = this.creationDate,
            modificationDate = this.modificationDate,
            eventType = this.eventType,
            startDate = this.startDate,
            endDate = this.endDate,
        )
}

enum class ReadingEventType {
    FINISHED,
    DROPPED,
    CURRENTLY_READING,
}
