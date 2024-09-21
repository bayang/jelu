package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.SeriesRatingDto
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.UUID

object SeriesRatingTable : UUIDTable("series_rating") {
    val creationDate = timestamp("creation_date")
    val modificationDate = timestamp("modification_date")
    val user = reference("user", UserTable, onDelete = ReferenceOption.CASCADE)
    val series = reference("series", SeriesTable, onDelete = ReferenceOption.CASCADE)
    val rating: Column<Double> = double(name = "rating")
}
class SeriesRating(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<SeriesRating>(SeriesRatingTable)
    var creationDate by SeriesRatingTable.creationDate
    var modificationDate by SeriesRatingTable.modificationDate
    var user by User referencedOn SeriesRatingTable.user
    var series by Series referencedOn SeriesRatingTable.series
    var rating by SeriesRatingTable.rating

    fun toSeriesRatingDto(): SeriesRatingDto = SeriesRatingDto(
        creationDate = this.creationDate,
        modificationDate = this.modificationDate,
        rating = this.rating,
        userId = this.user.id.value,
        seriesId = this.series.id.value,
    )
}
