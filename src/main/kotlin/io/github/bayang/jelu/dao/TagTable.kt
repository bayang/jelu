package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.TagDto
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.UUID

object TagTable : UUIDTable("tag") {
    val name: Column<String> = varchar("name", 1000)
    val creationDate = timestamp("creation_date")
    val modificationDate = timestamp("modification_date")
}

class Tag(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Tag>(TagTable)

    var name by TagTable.name
    var creationDate by TagTable.creationDate
    var modificationDate by TagTable.modificationDate

    fun toTagDto(): TagDto =
        TagDto(
            id = this.id.value,
            creationDate = this.creationDate,
            modificationDate = this.modificationDate,
            name = this.name,
        )
}

object BookTags : Table(name = "book_tags") {
    val book = reference("book", BookTable, fkName = "fk_booktags_book_id", onDelete = ReferenceOption.CASCADE)
    val tag = reference("tag", TagTable, fkName = "fk_booktags_tag_id", onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(book, tag, name = "pk_booktag_act")
}
