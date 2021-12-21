package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.TagDto
import io.github.bayang.jelu.dto.TagWithBooksDto
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.*

object TagTable: UUIDTable("tag") {
    val name: Column<String> = varchar("name", 1000)
    val creationDate = timestamp("creation_date")
    val modificationDate = timestamp("modification_date")
}
class Tag(id: EntityID<UUID>): UUIDEntity(id) {
    companion object : UUIDEntityClass<Tag>(TagTable)
    var name by TagTable.name
    var creationDate by TagTable.creationDate
    var modificationDate by TagTable.modificationDate
    var books by Book via BookTags
    fun toTagDto(): TagDto =
        TagDto(
            id = this.id.value,
            creationDate = this.creationDate,
            modificationDate = this.modificationDate,
            name = this.name
        )
    fun toTagWithBooksDto(): TagWithBooksDto =
        TagWithBooksDto(
            id = this.id.value,
            creationDate = this.creationDate,
            modificationDate = this.modificationDate,
            name = this.name,
            books = this.books.map { it.toBookDto() }
        )
}
object BookTags : Table(name = "book_tags") {
    val book = reference("book", BookTable, fkName = "fk_booktags_book_id")
    val tag = reference("tag", TagTable, fkName = "fk_booktags_tag_id")
    override val primaryKey = PrimaryKey(book, tag, name = "pk_booktag_act")
}
