package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.CustomListDto
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.UUID

object CustomListTable : UUIDTable("custom_list") {
    val creationDate = timestamp("creation_date")
    val modificationDate = timestamp("modification_date")
    val user = reference("user", UserTable, onDelete = ReferenceOption.CASCADE)
    val name: Column<String> = varchar("name", 5000)
    val tags: Column<String> = varchar("tags", 5000)
    val public: Column<Boolean> = bool("public")
    val actionable: Column<Boolean> = bool("actionable")
}
class CustomList(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<CustomList>(CustomListTable)
    var creationDate by CustomListTable.creationDate
    var modificationDate by CustomListTable.modificationDate
    var user by User referencedOn CustomListTable.user
    var name by CustomListTable.name
    var tags by CustomListTable.tags
    var public by CustomListTable.public
    var actionable by CustomListTable.actionable

    fun toCustomListDto(): CustomListDto = CustomListDto(
        id = this.id.value,
        name = this.name,
        creationDate = this.creationDate,
        modificationDate = this.modificationDate,
        tags = this.tags,
        public = this.public,
        actionable = this.actionable,
    )
}
