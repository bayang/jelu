package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dao.User.Companion.referrersOn
import io.github.bayang.jelu.dto.BookDto
import io.github.bayang.jelu.dto.BookDtoWithEvents
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.*

object BookTable: UUIDTable("book") {
    val title: Column<String> = varchar("title", 1000)
    val creationDate = timestamp("creation_date")
    val modificationDate = timestamp("modification_date")
    val isbn10: Column<String?> = varchar("isbn10", 20).nullable()
    val isbn13: Column<String?> = varchar("isbn13", 20).nullable()
    val publisher: Column<String?> = varchar("publisher", 500).nullable()
    val publishedDate: Column<String?> = varchar("published_date", 50).nullable()
    val summary: Column<String?> = varchar("summary", 50000).nullable()
    val pageCount: Column<Int?> = integer(name = "page_count").nullable()
    val image: Column<String?> = varchar("image", 1000).nullable()
}
class Book(id: EntityID<UUID>): UUIDEntity(id) {
    companion object : UUIDEntityClass<Book>(BookTable)
    var title by BookTable.title
    var creationDate by BookTable.creationDate
    var modificationDate by BookTable.modificationDate
    var isbn10 by BookTable.isbn10
    var isbn13 by BookTable.isbn13
    var summary by BookTable.summary
    var pageCount by BookTable.pageCount
    var publishedDate by BookTable.publishedDate
    var publisher by BookTable.publisher
    var authors by Author via BookAuthors
    var image by BookTable.image
    val userBooks by UserBook referrersOn UserBookTable.book
//    val readingEvents by ReadingEvent referrersOn ReadingEventTable.book // make sure to use val and referrersOn
    fun toBookDto(): BookDto =
        BookDto(
            id = this.id.value,
            creationDate = this.creationDate,
            title = this.title,
            isbn10 = this.isbn10,
            isbn13 = this.isbn13,
            summary = this.summary,
            image = this.image,
            publisher = this.publisher,
            publishedDate = this.publishedDate,
            pageCount = this.pageCount,
            modificationDate = this.modificationDate,
            authors = this.authors.map { it.toAuthorDto() }
        )
//    fun toBookWithReadingEventsDto(): BookDtoWithEvents =
//        BookDtoWithEvents(
//            id = this.id.value,
//            creationDate = this.creationDate,
//            title = this.title,
//            isbn10 = this.isbn10,
//            isbn13 = this.isbn13,
//            summary = this.summary,
//            image = this.image,
//            publisher = this.publisher,
//            publishedDate = this.publishedDate,
//            pageCount = this.pageCount,
//            modificationDate = this.modificationDate,
//            authors = this.authors.map { it.toAuthorDto() },
//            readingEvents = this.readingEvents.map { it.toReadingEventWithoutUserAndBookDto() }
//        )
//    fun toBookWithReadingEventsDto(user: User): BookDtoWithEvents =
//        BookDtoWithEvents(
//            id = this.id.value,
//            creationDate = this.creationDate,
//            title = this.title,
//            isbn10 = this.isbn10,
//            isbn13 = this.isbn13,
//            summary = this.summary,
//            image = this.image,
//            publisher = this.publisher,
//            publishedDate = this.publishedDate,
//            pageCount = this.pageCount,
//            modificationDate = this.modificationDate,
//            authors = this.authors.map { it.toAuthorDto() },
//            readingEvents = this.readingEvents.filter { it.user.id.value == user.id.value }.map { it.toReadingEventWithoutUserAndBookDto() }
//        )
}
