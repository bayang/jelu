package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.UserBookDto
import io.github.bayang.jelu.dto.UserBookLightDto
import io.github.bayang.jelu.dto.UserBookLightWithoutBookDto
import io.github.bayang.jelu.dto.UserBookWithoutEventsAndUserDto
import io.github.bayang.jelu.dto.UserBookWithoutEventsDto
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.UUID

object UserBookTable : UUIDTable("user_book") {
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
    val currentPageNumber: Column<Int?> = integer(name = "current_page_number").nullable()
    val borrowed: Column<Boolean?> = bool("is_borrowed").nullable()
}

class UserBook(id: EntityID<UUID>) : UUIDEntity(id) {
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
    var currentPageNumber by UserBookTable.currentPageNumber
    var borrowed by UserBookTable.borrowed
    var avgRating: Double? = null
    var userAvgRating: Double? = null

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
            currentPageNumber = this.currentPageNumber,
            borrowed = this.borrowed,
            readingEvents = this.readingEvents.map { it.toReadingEventWithoutUserBookDto() },
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
            currentPageNumber = this.currentPageNumber,
            borrowed = this.borrowed,
            readingEvents = this.readingEvents.map { it.toReadingEventWithoutUserBookDto() },
        )
    fun toUserBookLightWithoutBookDto(): UserBookLightWithoutBookDto =
        UserBookLightWithoutBookDto(
            id = this.id.value,
            creationDate = this.creationDate,
            modificationDate = this.modificationDate,
            owned = this.owned,
            toRead = this.toRead,
            personalNotes = this.personalNotes,
            lastReadingEvent = this.lastReadingEvent,
            lastReadingEventDate = this.lastReadingEventDate,
            percentRead = this.percentRead,
            currentPageNumber = this.currentPageNumber,
            borrowed = this.borrowed,
            readingEvents = this.readingEvents.map { it.toReadingEventWithoutUserBookDto() },
        )
    fun toUserBookWthoutEventsAndUserDto(): UserBookWithoutEventsAndUserDto =
        UserBookWithoutEventsAndUserDto(
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
            currentPageNumber = this.currentPageNumber,
            borrowed = this.borrowed,
            avgRating = this.avgRating,
            userAvgRating = this.userAvgRating,
        )
    fun toUserBookWithoutEventsDto(): UserBookWithoutEventsDto =
        UserBookWithoutEventsDto(
            id = this.id.value,
            creationDate = this.creationDate,
            modificationDate = this.modificationDate,
            book = this.book.toBookDto(),
            owned = this.owned,
            toRead = this.toRead,
            personalNotes = this.personalNotes,
            user = this.user.toUserDto(),
            percentRead = this.percentRead,
            currentPageNumber = this.currentPageNumber,
            borrowed = this.borrowed,
        )
}
