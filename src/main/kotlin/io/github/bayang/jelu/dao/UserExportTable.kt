package io.github.bayang.jelu.dao

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.javatime.timestamp
import java.util.UUID

object UserExportTable : UUIDTable("user_export") {
    val creationDate = timestamp("creation_date")
    val modificationDate = timestamp("modification_date")
    val user = reference("user", UserTable, onDelete = ReferenceOption.CASCADE)
    val name: Column<String> = varchar("name", 500)
}

class UserExport(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<UserExport>(UserExportTable)

    var creationDate by UserExportTable.creationDate
    var modificationDate by UserExportTable.modificationDate
    var user by User referencedOn UserExportTable.user
    var name by UserExportTable.name
}
