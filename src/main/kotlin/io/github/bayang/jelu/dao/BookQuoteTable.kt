package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.BookQuoteDto
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.UUID

object BookQuoteTable : UUIDTable("book_quote") {
    val creationDate = timestamp("creation_date")
    val modificationDate = timestamp("modification_date")
    val user = reference("user", UserTable, onDelete = ReferenceOption.CASCADE)
    val book = reference("book", BookTable, onDelete = ReferenceOption.CASCADE)
    val text: Column<String> = varchar("text", 500000)
    val visibility = enumerationByName("visibility", 200, Visibility::class)
    val position: Column<String?> = varchar("position", 300).nullable()
}
class BookQuote(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<BookQuote>(BookQuoteTable)
    var creationDate by BookQuoteTable.creationDate
    var modificationDate by BookQuoteTable.modificationDate
    var user by User referencedOn BookQuoteTable.user
    var book by Book referencedOn BookQuoteTable.book
    var text by BookQuoteTable.text
    var visibility by BookQuoteTable.visibility
    var position by BookQuoteTable.position

    fun toBookQuoteDto(): BookQuoteDto = BookQuoteDto(
        id = this.id.value,
        creationDate = this.creationDate,
        modificationDate = this.modificationDate,
        text = this.text,
        visibility = this.visibility,
        user = this.user.id.value,
        book = this.book.id.value,
        position = this.position,
    )
}
