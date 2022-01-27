package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.CreateReadingEventDto
import io.github.bayang.jelu.dto.UpdateReadingEventDto
import io.github.bayang.jelu.errors.JeluException
import io.github.bayang.jelu.utils.nowInstant
import mu.KotlinLogging
import org.jetbrains.exposed.sql.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

private val logger = KotlinLogging.logger {}

@Repository
class ReadingEventRepository {

    fun findAll(
        searchTerm: ReadingEventType?,
        userId: UUID?,
        page: Long = 0,
        pageSize: Long = 20
    ): PageImpl<ReadingEvent> {
        if (userId != null) {
            return findAllByUser(userId, searchTerm, page, pageSize)
        }
        val query = ReadingEventTable.selectAll()
        searchTerm?.let {
            query.andWhere { ReadingEventTable.eventType eq searchTerm }
        }
        val total = query.count()
        query.limit(pageSize.toInt(), page * pageSize)
        val pageRequest = PageRequest.of(page.toInt(), pageSize.toInt(), Sort.by(Sort.Order.desc("createdDate")))
        return PageImpl(
            ReadingEvent.wrapRows(query).toList(),
            if (pageRequest.isPaged) PageRequest.of(pageRequest.pageNumber, pageRequest.pageSize, Sort.unsorted())
            else PageRequest.of(0, 20, Sort.unsorted()),
            total
        )
    }

    fun findAllByUser(
        userID: UUID,
        searchTerm: ReadingEventType? = null,
        page: Long = 0,
        pageSize: Long = 20
    ): PageImpl<ReadingEvent> {
        val query = UserBookTable.innerJoin(ReadingEventTable)
            .slice(ReadingEventTable.columns)
            .select { UserBookTable.user eq userID }

        searchTerm?.let {
            query.andWhere { ReadingEventTable.eventType eq searchTerm }
        }

        val total = query.count()
        query.limit(pageSize.toInt(), page * pageSize)
        query.orderBy(Pair(ReadingEventTable.modificationDate, SortOrder.DESC_NULLS_LAST))
        val res = ReadingEvent.wrapRows(query).toList()

        val pageRequest = PageRequest.of(page.toInt(), pageSize.toInt(), Sort.by(Sort.Order.desc("createdDate")))
        return PageImpl(
            res,
            if (pageRequest.isPaged) PageRequest.of(pageRequest.pageNumber, pageRequest.pageSize, Sort.unsorted())
            else PageRequest.of(0, 20, Sort.unsorted()),
            total
        )
    }

    fun save(createReadingEventDto: CreateReadingEventDto, targetUser: User): ReadingEvent {
        if (createReadingEventDto.bookId == null) {
            throw JeluException("Missing bookId to create reading event")
        }
        val foundBook: Book = Book[createReadingEventDto.bookId]
        return save(createReadingEventDto, foundBook, targetUser)
    }

    fun save(createReadingEventDto: CreateReadingEventDto, book: Book, targetUser: User): ReadingEvent {
        var found: UserBook? =
            UserBook.find { UserBookTable.user eq targetUser.id and (UserBookTable.book.eq(book.id)) }.firstOrNull()
        val instant: Instant = nowInstant()
        if (found == null) {
            found = UserBook.new {
                this.creationDate = instant
                this.user = targetUser
                this.book = book
            }
        }
        found.modificationDate = instant
        return save(found, createReadingEventDto)
    }

    fun save(userBook: UserBook, createReadingEventDto: CreateReadingEventDto): ReadingEvent {
        val alreadyReadingEvent: ReadingEvent? =
            userBook.readingEvents.find { it.eventType == ReadingEventType.CURRENTLY_READING }
        val instant: Instant = nowInstant()
        if (userBook.lastReadingEvent != null) {
            if (createReadingEventDto.readDate != null && createReadingEventDto.readDate.isAfter(userBook.lastReadingEventDate)) {
                userBook.lastReadingEvent = createReadingEventDto.eventType
                userBook.lastReadingEventDate = createReadingEventDto.readDate
            }
        } else {
            userBook.lastReadingEvent = createReadingEventDto.eventType
            userBook.lastReadingEventDate = createReadingEventDto.readDate ?: instant
        }
        if (alreadyReadingEvent != null) {
            if (createReadingEventDto.readDate == null || createReadingEventDto.readDate.isAfter(alreadyReadingEvent.creationDate)) {
                logger.debug { "found ${userBook.readingEvents.count()} older events in CURRENTLY_READING state for book ${userBook.book.id}" }
                alreadyReadingEvent.eventType = createReadingEventDto.eventType
                alreadyReadingEvent.modificationDate = instant
                return alreadyReadingEvent
            }
        }
        return ReadingEvent.new {
            this.creationDate = createReadingEventDto.readDate ?: instant
            this.modificationDate = instant
            this.eventType = createReadingEventDto.eventType
            this.userBook = userBook
        }
    }

    fun updateReadingEvent(readingEventId: UUID, updateReadingEventDto: UpdateReadingEventDto): ReadingEvent {
        return ReadingEvent[readingEventId].apply {
            this.modificationDate = nowInstant()
            this.eventType = updateReadingEventDto.eventType
            this.userBook.lastReadingEvent = updateReadingEventDto.eventType
            this.userBook.lastReadingEventDate = this.modificationDate
        }
    }

    fun deleteReadingEventById(eventId: UUID) {
        val entity: ReadingEvent = ReadingEvent[eventId]
        val userbook = entity.userBook
        entity.delete()
        val lastEvent = userbook.readingEvents
            .maxByOrNull { e -> e.modificationDate }
        if (lastEvent == null) {
            userbook.lastReadingEvent = null
            userbook.lastReadingEventDate = null
        } else {
            userbook.lastReadingEventDate = lastEvent.modificationDate
            userbook.lastReadingEvent = lastEvent.eventType
        }
    }
}
