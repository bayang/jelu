package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.SeriesDto
import io.github.bayang.jelu.dto.SeriesOrderDto
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnSet
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.UUID

object SeriesTable : UUIDTable("series") {
    val name: Column<String> = varchar("name", 1000)
    val creationDate = timestamp("creation_date")
    val modificationDate = timestamp("modification_date")
    val description: Column<String?> = varchar("description", 500000).nullable()
}
class Series(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Series>(SeriesTable)
    var name by SeriesTable.name
    var creationDate by SeriesTable.creationDate
    var modificationDate by SeriesTable.modificationDate
    var avgRating: Double? = null
    var userRating: Double? = null
    var description by SeriesTable.description
    fun toSeriesDto(): SeriesDto =
        SeriesDto(
            id = this.id.value,
            creationDate = this.creationDate,
            modificationDate = this.modificationDate,
            name = this.name,
            userRating = this.userRating,
            avgRating = this.avgRating,
            description = this.description,
        )
}
object BookSeries : UUIDTable(name = "book_series") {
    val book = reference("book", BookTable, fkName = "fk_bookseries_book_id", onDelete = ReferenceOption.CASCADE)
    val series = reference("series", SeriesTable, fkName = "fk_bookseries_series_id", onDelete = ReferenceOption.CASCADE)
    val numberInSeries: Column<Double?> = double(name = "number_in_series").nullable()
    init {
        // actually the work is done in the liquibase part
        // this is only to convey the intent
        uniqueIndex(book, series, numberInSeries)
    }
}
class BookSeriesItem(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<BookSeriesItem>(BookSeries) {
        override val dependsOnTables: ColumnSet = BookTable.innerJoin(BookSeries).innerJoin(SeriesTable)
        override fun createInstance(entityId: EntityID<UUID>, row: ResultRow?): BookSeriesItem {
            row?.getOrNull(BookTable.id)?.let {
                Book.wrap(it, row)
            }
            row?.getOrNull(SeriesTable.id)?.let {
                Series.wrap(it, row)
            }
            return super.createInstance(entityId, row)
        }
    }
    var book by Book referencedOn BookSeries.book
    var series by Series referencedOn BookSeries.series
    var numberInSeries by BookSeries.numberInSeries
    fun toSeriesOrderDto(): SeriesOrderDto = SeriesOrderDto(
        seriesId = this.series.id.value,
        name = this.series.name,
        numberInSeries = this.numberInSeries,
    )
}
