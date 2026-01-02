package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.LifeCycleDto
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp

object LifeCycleTable : LongIdTable("lifecycle") {
    val creationDate = timestamp("creation_date")
    val modificationDate = timestamp("modification_date")
    val seriesMigrated: Column<Boolean> = bool("series_migrated")
}

class LifeCycle(
    id: EntityID<Long>,
) : LongEntity(id) {
    companion object : LongEntityClass<LifeCycle>(LifeCycleTable)

    var creationDate by LifeCycleTable.creationDate
    var modificationDate by LifeCycleTable.modificationDate
    var seriesMigrated by LifeCycleTable.seriesMigrated

    fun toLifeCycleDto(): LifeCycleDto =
        LifeCycleDto(
            creationDate = this.creationDate,
            modificationDate = this.modificationDate,
            seriesMigrated = this.seriesMigrated,
            id = this.id.value,
        )
}
