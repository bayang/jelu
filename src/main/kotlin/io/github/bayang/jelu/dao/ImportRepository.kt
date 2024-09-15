package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.ImportDto
import io.github.bayang.jelu.utils.nowInstant
import mu.KotlinLogging
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}
const val DEFAULT_BLOCK_SIZE: Int = 50

@Repository
class ImportRepository {

    fun deleteByprocessingStatusAndUser(
        processingStatus: ProcessingStatus,
        userId: UUID,
    ): Int {
        // FIXME SQLite doesn't support LIMIT in DELETE clause., dialect: sqlite.
        return ImportEntityTable.deleteWhere {
            ImportEntityTable.processingStatus eq processingStatus and (ImportEntityTable.userId eq userId)
        }
    }

    fun getByprocessingStatusAndUser(
        processingStatus: ProcessingStatus,
        userId: UUID,
    ): List<ImportEntity> {
        return ImportEntity.find { ImportEntityTable.processingStatus eq processingStatus and (ImportEntityTable.userId eq userId) }
            .limit(DEFAULT_BLOCK_SIZE)
            .toList()
    }

    fun countByprocessingStatusAndUser(
        processingStatus: ProcessingStatus,
        userId: UUID,
    ): Long = ImportEntity.count(ImportEntityTable.processingStatus eq processingStatus and(ImportEntityTable.userId eq userId))

    fun save(
        entity: ImportDto,
        processingStatus: ProcessingStatus,
        userId: UUID,
        shouldFetchMetadata: Boolean,
    ): ImportEntity {
        return ImportEntity.new {
            this.processingStatus = processingStatus
            val instant: Instant = nowInstant()
            this.creationDate = instant
            this.modificationDate = instant
            this.authors = entity.authors
            this.isbn10 = entity.isbn10
            this.isbn13 = entity.isbn13
            this.goodreadsId = entity.goodreadsId
            this.tags = entity.tags.joinToString(separator = ",")
            this.numberOfPages = entity.numberOfPages
            this.personalNotes = entity.personalNotes
            this.publishedDate = entity.publishedDate
            this.publisher = entity.publisher
            this.title = entity.title
            this.readDates = entity.readDates
            this.readCount = entity.readCount
            this.userId = userId
            this.shouldFetchMetadata = shouldFetchMetadata
            this.importSource = entity.importSource!!
            this.librarythingId = entity.librarythingId
            this.owned = entity.owned
            this.rating = entity.rating
            this.review = entity.review
        }
    }

    fun updateStatus(
        oldStatus: ProcessingStatus,
        newStatus: ProcessingStatus,
        userId: UUID,
    ): Int {
        return ImportEntityTable.update({ ImportEntityTable.processingStatus eq oldStatus and (ImportEntityTable.userId eq userId) }) {
            it[processingStatus] = newStatus
        }
    }

    fun updateStatus(entityId: UUID, newStatus: ProcessingStatus): Int {
        return ImportEntityTable.update({ ImportEntityTable.id eq entityId }) {
            it[processingStatus] = newStatus
        }
    }
}
