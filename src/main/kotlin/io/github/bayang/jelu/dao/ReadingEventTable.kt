package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.ReadingEventDto
import io.github.bayang.jelu.dto.ReadingEventWithoutUserDto
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.*

object ReadingEventTable: UUIDTable("reading_event") {
    val creationDate = timestamp("creation_date")
    val modificationDate = timestamp("modification_date")
    val user = reference("user", UserTable)
    val eventType = enumerationByName("event_type", 200, ReadingEventType::class)
    val book = reference("book", BookTable)
}
class ReadingEvent(id: EntityID<UUID>): UUIDEntity(id) {
    companion object : UUIDEntityClass<ReadingEvent>(ReadingEventTable)
    var creationDate by ReadingEventTable.creationDate
    var modificationDate by ReadingEventTable.modificationDate
    var user by User referencedOn ReadingEventTable.user
    var eventType by ReadingEventTable.eventType
    var book by Book referencedOn ReadingEventTable.book

    fun toReadingEventDto(): ReadingEventDto = ReadingEventDto(
        id = this.id.value,
        creationDate = this.creationDate,
        modificationDate = this.modificationDate,
        user = this.user.toUserDto(),
        book = this.book.toBookDto(),
        eventType = this.eventType
    )
    fun toReadingEventWithoutUserDto(): ReadingEventWithoutUserDto = ReadingEventWithoutUserDto(
        id = this.id.value,
        creationDate = this.creationDate,
        modificationDate = this.modificationDate,
        book = this.book.toBookDto(),
        eventType = this.eventType
    )
}
enum class ReadingEventType {
    FINISHED,
    DROPPED,
    CURRENTLY_READING
}