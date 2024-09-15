package io.github.bayang.jelu.dao

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.UUID

object ImportEntityTable : UUIDTable("import_entity") {
    val creationDate = timestamp("creation_date")
    val modificationDate = timestamp("modification_date")
    val processingStatus = enumerationByName("processing_status", 100, ProcessingStatus::class)
    val importSource = enumerationByName("import_source", 100, ImportSource::class)
    var goodreadsId: Column<String?> = varchar("goodreads_id", 50).nullable()
    var librarythingId: Column<String?> = varchar("librarything_id", 50).nullable()
    var title: Column<String?> = varchar("title", 1000).nullable()
    var authors: Column<String?> = varchar("authors", 10000).nullable()
    var isbn10: Column<String?> = varchar("isbn10", 20).nullable()
    var isbn13: Column<String?> = varchar("isbn13", 20).nullable()
    var publisher: Column<String?> = varchar("publisher", 500).nullable()
    var numberOfPages: Column<Int?> = integer(name = "number_of_pages").nullable()
    var publishedDate: Column<String?> = varchar("published_date", 50).nullable()
    var readDates: Column<String?> = varchar("read_dates", 1000).nullable()
    var tags: Column<String?> = varchar("tags", 10000).nullable()
    var personalNotes: Column<String?> = varchar("personal_notes", 10000).nullable()
    var readCount: Column<Int?> = integer(name = "read_count").nullable()
    var userId: Column<UUID> = uuid(name = "userId")
    var shouldFetchMetadata: Column<Boolean> = bool("should_fetch_metadata")
    var owned: Column<Boolean?> = bool("owned").nullable()
    var rating: Column<Int?> = integer(name = "rating").nullable()
    var review: Column<String?> = varchar("review", 500000).nullable()
}

class ImportEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ImportEntity>(ImportEntityTable)
    var creationDate by ImportEntityTable.creationDate
    var modificationDate by ImportEntityTable.modificationDate
    var processingStatus by ImportEntityTable.processingStatus
    var importSource by ImportEntityTable.importSource
    var goodreadsId by ImportEntityTable.goodreadsId
    var librarythingId by ImportEntityTable.librarythingId
    var title by ImportEntityTable.title
    var authors by ImportEntityTable.authors
    var isbn10 by ImportEntityTable.isbn10
    var isbn13 by ImportEntityTable.isbn13
    var publisher by ImportEntityTable.publisher
    var numberOfPages by ImportEntityTable.numberOfPages
    var publishedDate by ImportEntityTable.publishedDate
    var readDates by ImportEntityTable.readDates
    var tags by ImportEntityTable.tags
    var personalNotes by ImportEntityTable.personalNotes
    var readCount by ImportEntityTable.readCount
    var userId by ImportEntityTable.userId
    var shouldFetchMetadata by ImportEntityTable.shouldFetchMetadata
    var owned by ImportEntityTable.owned
    var rating by ImportEntityTable.rating
    var review by ImportEntityTable.review
}

enum class ProcessingStatus {
    SAVED,
    PROCESSING,
    IMPORTED,
    ERROR,
}
enum class ImportSource {
    GOODREADS,
    STORYGRAPH,
    LIBRARYTHING,
    ISBN_LIST,
}
