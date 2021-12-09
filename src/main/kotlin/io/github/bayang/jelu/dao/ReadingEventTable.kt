package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.ReadingEventDto
import io.github.bayang.jelu.dto.ReadingEventWithoutUserBookDto
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.*

object ReadingEventTable: UUIDTable("reading_event") {
    val creationDate = timestamp("creation_date")
    val modificationDate = timestamp("modification_date")
    val userBook = reference("user_book", UserBookTable)
    val eventType = enumerationByName("event_type", 200, ReadingEventType::class)
}
class ReadingEvent(id: EntityID<UUID>): UUIDEntity(id) {
    companion object : UUIDEntityClass<ReadingEvent>(ReadingEventTable)
    var creationDate by ReadingEventTable.creationDate
    var modificationDate by ReadingEventTable.modificationDate
    var userBook by UserBook referencedOn ReadingEventTable.userBook
    var eventType by ReadingEventTable.eventType

    fun toReadingEventDto(): ReadingEventDto = ReadingEventDto(
        id = this.id.value,
        creationDate = this.creationDate,
        modificationDate = this.modificationDate,
        userBook = this.userBook.toUserBookWithoutEventsDto(),
        eventType = this.eventType
    )
    fun toReadingEventWithoutUserBookDto(): ReadingEventWithoutUserBookDto = ReadingEventWithoutUserBookDto(
        id = this.id.value,
        creationDate = this.creationDate,
        modificationDate = this.modificationDate,
        eventType = this.eventType
    )
}
enum class ReadingEventType {
    FINISHED,
    DROPPED,
    CURRENTLY_READING
}