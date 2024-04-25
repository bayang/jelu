package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.BookDto
import io.github.bayang.jelu.dto.BookUpdateDto
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.UUID

object BookTable : UUIDTable("book") {
    val title: Column<String> = varchar("title", 1000)
    val creationDate = timestamp("creation_date")
    val modificationDate = timestamp("modification_date")
    val isbn10: Column<String?> = varchar("isbn10", 20).nullable()
    val isbn13: Column<String?> = varchar("isbn13", 20).nullable()
    val publisher: Column<String?> = varchar("publisher", 500).nullable()
    val publishedDate: Column<String?> = varchar("published_date", 50).nullable()
    val summary: Column<String?> = varchar("summary", 50000).nullable()
    val pageCount: Column<Int?> = integer(name = "page_count").nullable()
    val image: Column<String?> = varchar("image", 1000).nullable()
    val seriesBak: Column<String?> = varchar("series_bak", 500).nullable()
    val numberInSeries: Column<Double?> = double(name = "number_in_series").nullable()
    val googleId: Column<String?> = varchar("google_id", 30).nullable()
    val goodreadsId: Column<String?> = varchar("goodreads_id", 30).nullable()
    val amazonId: Column<String?> = varchar("amazon_id", 30).nullable()
    val librarythingId: Column<String?> = varchar("librarything_id", 30).nullable()
    val language: Column<String?> = varchar("language", 30).nullable()
}
class Book(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Book>(BookTable)
    var title by BookTable.title
    var creationDate by BookTable.creationDate
    var modificationDate by BookTable.modificationDate
    var isbn10 by BookTable.isbn10
    var isbn13 by BookTable.isbn13
    var summary by BookTable.summary
    var pageCount by BookTable.pageCount
    var publishedDate by BookTable.publishedDate
    var publisher by BookTable.publisher
    var authors by Author via BookAuthors
    var translators by Author via BookTranslators
    var tags by Tag via BookTags
    var image by BookTable.image
    var seriesBak by BookTable.seriesBak
    val seriesAndOrder by BookSeriesItem.referrersOn(BookSeries.book)
    var language by BookTable.language
    var numberInSeries by BookTable.numberInSeries
    var googleId by BookTable.googleId
    var amazonId by BookTable.amazonId
    var goodreadsId by BookTable.goodreadsId
    var librarythingId by BookTable.librarythingId
    val userBooks by UserBook referrersOn UserBookTable.book
    var userBookId: UUID? = null
    var userBook: UserBook? = null

    fun toBookDto(): BookDto =
        BookDto(
            id = this.id.value,
            creationDate = this.creationDate,
            title = this.title,
            isbn10 = this.isbn10,
            isbn13 = this.isbn13,
            summary = this.summary,
            image = this.image,
            publisher = this.publisher,
            publishedDate = this.publishedDate,
            pageCount = this.pageCount,
            modificationDate = this.modificationDate,
            // seriesBak = this.seriesBak,
            // numberInSeries = this.numberInSeries,
            goodreadsId = this.goodreadsId,
            googleId = this.googleId,
            amazonId = this.amazonId,
            librarythingId = this.librarythingId,
            language = this.language,
            authors = this.authors.map { it.toAuthorDto() },
            translators = this.translators.map { it.toAuthorDto() },
            tags = this.tags.map { it.toTagDto() },
            userBookId = this.userBookId,
            series = this.seriesAndOrder.map { it.toSeriesOrderDto() },
            userbook = this.userBook?.toUserBookLightWithoutBookDto(),
        )

    fun toBookUpdateDto(): BookUpdateDto =
        BookUpdateDto(
            title = this.title,
            isbn10 = this.isbn10,
            isbn13 = this.isbn13,
            summary = this.summary,
            image = this.image,
            publisher = this.publisher,
            publishedDate = this.publishedDate,
            pageCount = this.pageCount,
            // seriesBak = this.seriesBak,
            // numberInSeries = this.numberInSeries,
            goodreadsId = this.goodreadsId,
            googleId = this.googleId,
            amazonId = this.amazonId,
            librarythingId = this.librarythingId,
            language = this.language,
            authors = this.authors.map { it.toAuthorDto() },
            translators = this.translators.map { it.toAuthorDto() },
            tags = this.tags.map { it.toTagDto() },
            series = this.seriesAndOrder.map { it.toSeriesOrderDto() },
        )
}
