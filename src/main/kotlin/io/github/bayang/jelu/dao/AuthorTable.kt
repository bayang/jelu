package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.AuthorDto
import io.github.bayang.jelu.dto.AuthorWithBooksDto
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.*

object AuthorTable: UUIDTable("author") {
    val name: Column<String> = varchar("name", 1000)
    val creationDate = timestamp("creation_date")
    val modificationDate = timestamp("modification_date")
}
class Author(id: EntityID<UUID>): UUIDEntity(id) {
    companion object : UUIDEntityClass<Author>(AuthorTable)
    var name by AuthorTable.name
    var creationDate by AuthorTable.creationDate
    var modificationDate by AuthorTable.modificationDate
    var books by Book via BookAuthors
    fun toAuthorDto(): AuthorDto =
        AuthorDto(
            id = this.id.value,
            creationDate = this.creationDate,
            modificationDate = this.modificationDate,
            name = this.name
        )
    fun toAuthorWithBooksDto(): AuthorWithBooksDto =
        AuthorWithBooksDto(
            id = this.id.value,
            creationDate = this.creationDate,
            modificationDate = this.modificationDate,
            name = this.name,
            books = this.books.map { it.toBookDto() }
        )
}
object BookAuthors : Table(name = "book_authors") {
    val book = reference("book", BookTable, fkName = "fk_bookauthors_book_id")
    val author = reference("author", AuthorTable, fkName = "fk_bookauthors_author_id")
    override val primaryKey = PrimaryKey(book, author, name = "pk_bookauthor_act")
}
