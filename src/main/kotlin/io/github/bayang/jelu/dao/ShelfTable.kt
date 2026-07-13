package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.ShelfDto
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.javatime.timestamp
import java.util.UUID

object ShelfTable : UUIDTable("shelf") {
    val creationDate = timestamp("creation_date")
    val modificationDate = timestamp("modification_date")
    val user = reference("user", UserTable, onDelete = ReferenceOption.CASCADE)
    val name: Column<String> = varchar("name", 5000)
    val targetId: Column<UUID> = javaUUID("target_id")
}

class Shelf(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Shelf>(ShelfTable)

    var creationDate by ShelfTable.creationDate
    var modificationDate by ShelfTable.modificationDate
    var user by User referencedOn ShelfTable.user
    var name by ShelfTable.name
    var targetId by ShelfTable.targetId

    fun toShelfDto(): ShelfDto =
        ShelfDto(
            id = this.id.value,
            name = this.name,
            creationDate = this.creationDate,
            modificationDate = this.modificationDate,
            targetId = this.targetId,
        )
}
