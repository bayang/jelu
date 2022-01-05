package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dao.Author.Companion.backReferencedOn
import io.github.bayang.jelu.dao.BookTable.nullable
import io.github.bayang.jelu.dto.UserBookDto
import io.github.bayang.jelu.dto.UserBookLightDto
import io.github.bayang.jelu.dto.UserBookWithoutBookDto
import io.github.bayang.jelu.dto.UserBookWithoutEventsDto
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

object UserBookTable: UUIDTable("user_book") {
    val creationDate = timestamp("creation_date")
    val modificationDate = timestamp("modification_date")
    val user = reference("user", UserTable, onDelete = ReferenceOption.CASCADE)
    val book = reference("book", BookTable, onDelete = ReferenceOption.CASCADE)
    val lastReadingEvent: Column<ReadingEventType?> = enumerationByName("last_reading_event", 200, ReadingEventType::class).nullable()
    val lastReadingEventDate: Column<Instant?> = timestamp("last_reading_event_date").nullable()
    val personalNotes: Column<String?> = varchar("notes", 5000).nullable()
    val owned: Column<Boolean?> = bool("is_owned").nullable()
    val toRead: Column<Boolean?> = bool("to_read").nullable()
    val percentRead: Column<Int?> = integer(name = "percent_read").nullable()
}

class UserBook(id: EntityID<UUID>): UUIDEntity(id) {
    companion object : UUIDEntityClass<UserBook>(UserBookTable)
    var creationDate by UserBookTable.creationDate
    var modificationDate by UserBookTable.modificationDate
    var user by User referencedOn UserBookTable.user
    var book by Book referencedOn UserBookTable.book
    var personalNotes by UserBookTable.personalNotes
    var owned by UserBookTable.owned
    var toRead by UserBookTable.toRead
    val readingEvents by ReadingEvent referrersOn ReadingEventTable.userBook // make sure to use val and referrersOn
    var lastReadingEventDate by UserBookTable.lastReadingEventDate
    var lastReadingEvent by UserBookTable.lastReadingEvent
    var percentRead by UserBookTable.percentRead

    fun toUserBookDto(): UserBookDto =
        UserBookDto(
            id = this.id.value,
            creationDate = this.creationDate,
            modificationDate = this.modificationDate,
            user = this.user.toUserDto(),
            book = this.book.toBookDto(),
            lastReadingEvent = this.lastReadingEvent,
            lastReadingEventDate = this.lastReadingEventDate,
            owned = this.owned,
            toRead = this.toRead,
            personalNotes = this.personalNotes,
            percentRead = this.percentRead,
            readingEvents = this.readingEvents.map { it.toReadingEventWithoutUserBookDto() }
        )
    fun toUserBookLightDto(): UserBookLightDto =
        UserBookLightDto(
            id = this.id.value,
            creationDate = this.creationDate,
            modificationDate = this.modificationDate,
            book = this.book.toBookDto(),
            owned = this.owned,
            toRead = this.toRead,
            personalNotes = this.personalNotes,
            lastReadingEvent = this.lastReadingEvent,
            lastReadingEventDate = this.lastReadingEventDate,
            percentRead = this.percentRead,
            readingEvents = this.readingEvents.map { it.toReadingEventWithoutUserBookDto() }
        )
    fun toUserBookWithoutEventsDto(): UserBookWithoutEventsDto=
        UserBookWithoutEventsDto(
            id = this.id.value,
            creationDate = this.creationDate,
            modificationDate = this.modificationDate,
            owned = this.owned,
            toRead = this.toRead,
            personalNotes = this.personalNotes,
            book = this.book.toBookDto(),
            user = this.user.toUserDto(),
            percentRead = this.percentRead,
        )

    fun toUserBookWithoutBookDto(): UserBookWithoutBookDto =
        UserBookWithoutBookDto(
            id = this.id.value,
            creationDate = this.creationDate,
            modificationDate = this.modificationDate,
            owned = this.owned,
            toRead = this.toRead,
            personalNotes = this.personalNotes,
            lastReadingEvent = this.lastReadingEvent,
            lastReadingEventDate = this.lastReadingEventDate,
            percentRead = this.percentRead,
            readingEvents = this.readingEvents.map { it.toReadingEventWithoutUserBookDto() }
        )
}
