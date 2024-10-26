package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.CreateReadingEventDto
import io.github.bayang.jelu.dto.UpdateReadingEventDto
import io.github.bayang.jelu.dto.UserDto
import io.github.bayang.jelu.errors.JeluException
import io.github.bayang.jelu.utils.nowInstant
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.javatime.year
import org.jetbrains.exposed.sql.selectAll
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Repository
class ReadingEventRepository {

    fun findAll(
        eventTypes: List<ReadingEventType>?,
        userId: UUID?,
        bookId: UUID?,
        startedAfter: LocalDate?,
        startedBefore: LocalDate?,
        endedAfter: LocalDate?,
        endedBefore: LocalDate?,
        pageable: Pageable,
    ): Page<ReadingEvent> {
        val query = ReadingEventTable.join(UserBookTable, JoinType.LEFT)
            .selectAll()
        if (eventTypes != null && eventTypes.isNotEmpty()) {
            query.andWhere { ReadingEventTable.eventType inList eventTypes }
        }
        if (userId != null) {
            query.andWhere { UserBookTable.user eq userId }
        }
        if (bookId != null) {
            query.andWhere { UserBookTable.book eq bookId }
        }
        if (endedBefore != null) {
            val instant = OffsetDateTime.of(endedBefore, LocalTime.MAX, ZoneId.systemDefault().rules.getOffset(nowInstant())).toInstant()
            query.andWhere { ReadingEventTable.endDate lessEq instant }
        }
        if (endedAfter != null) {
            val instant = OffsetDateTime.of(endedAfter, LocalTime.MIN, ZoneId.systemDefault().rules.getOffset(nowInstant())).toInstant()
            query.andWhere { ReadingEventTable.endDate greaterEq instant }
        }
        if (startedBefore != null) {
            val instant = OffsetDateTime.of(startedBefore, LocalTime.MAX, ZoneId.systemDefault().rules.getOffset(nowInstant())).toInstant()
            query.andWhere { ReadingEventTable.startDate lessEq instant }
        }
        if (startedAfter != null) {
            val instant = OffsetDateTime.of(startedAfter, LocalTime.MIN, ZoneId.systemDefault().rules.getOffset(nowInstant())).toInstant()
            query.andWhere { ReadingEventTable.startDate greaterEq instant }
        }
        val total = query.count()
        query.limit(pageable.pageSize, pageable.offset)
        val orders: Array<Pair<Expression<*>, SortOrder>> = parseSorts(pageable.sort, Pair(ReadingEventTable.modificationDate, SortOrder.DESC_NULLS_LAST), ReadingEventTable)
        query.orderBy(*orders)
        return PageImpl(
            ReadingEvent.wrapRows(query).toList(),
            pageable,
            total,
        )
    }

    fun findYears(
        eventTypes: List<ReadingEventType>?,
        userId: UUID?,
        bookId: UUID?,
    ): List<Int> {
        val query = ReadingEventTable.join(UserBookTable, JoinType.LEFT)
            .select(ReadingEventTable.endDate.year())
        if (eventTypes != null && eventTypes.isNotEmpty()) {
            query.andWhere { ReadingEventTable.eventType inList eventTypes }
        }
        if (userId != null) {
            query.andWhere { UserBookTable.user eq userId }
        }
        if (bookId != null) {
            query.andWhere { UserBookTable.book eq bookId }
        }
        query.withDistinct(true)
        // FIXME
        return query.map { resultRow -> resultRow[ReadingEventTable.endDate.year()] }.toList()
    }

    fun save(createReadingEventDto: CreateReadingEventDto, targetUser: UserDto): ReadingEvent {
        if (createReadingEventDto.bookId == null) {
            throw JeluException("Missing bookId to create reading event")
        }
        val foundBook: Book = Book[createReadingEventDto.bookId]
        return save(createReadingEventDto, foundBook, targetUser)
    }

    fun save(createReadingEventDto: CreateReadingEventDto, book: Book, targetUser: UserDto): ReadingEvent {
        var found: UserBook? =
            UserBook.find { UserBookTable.user eq targetUser.id and (UserBookTable.book.eq(book.id)) }.firstOrNull()
        val instant: Instant = nowInstant()
        if (found == null) {
            found = UserBook.new {
                this.creationDate = instant
                this.user = User[targetUser.id!!]
                this.book = book
            }
        }
        found.modificationDate = instant
        return save(found, createReadingEventDto)
    }

    fun save(userBook: UserBook, createReadingEventDto: CreateReadingEventDto): ReadingEvent {
        if (createReadingEventDto.startDate != null &&
            createReadingEventDto.eventDate != null &&
            createReadingEventDto.eventDate.isBefore(createReadingEventDto.startDate)
        ) {
            throw JeluException("start date cannot be after event date")
        }
        val alreadyReadingEvent: ReadingEvent? =
            userBook.readingEvents.find { it.eventType == ReadingEventType.CURRENTLY_READING }
        val instant: Instant = nowInstant()

        // we have a previous event,
        // only update lastReadingEvent if the new one has a date and is effectively after the one already existing
        if (userBook.lastReadingEventDate != null && createReadingEventDto.eventDate != null) {
            if (createReadingEventDto.eventDate.isAfter(userBook.lastReadingEventDate)) {
                userBook.lastReadingEvent = createReadingEventDto.eventType
                userBook.lastReadingEventDate = createReadingEventDto.eventDate
            }
            // no previous event, or the new one does not have a date set lastReadingEvent in any case
        } else if (userBook.lastReadingEventDate != null && createReadingEventDto.startDate != null) {
            if (createReadingEventDto.startDate.isAfter(userBook.lastReadingEventDate)) {
                userBook.lastReadingEvent = createReadingEventDto.eventType
                userBook.lastReadingEventDate = createReadingEventDto.startDate
            }
            // no previous event, or the new one does not have a date set lastReadingEvent in any case
        } else {
            userBook.lastReadingEvent = createReadingEventDto.eventType
            userBook.lastReadingEventDate = createReadingEventDto.eventDate ?: instant
        }
        if (alreadyReadingEvent != null) {
            if (createReadingEventDto.eventDate == null || createReadingEventDto.eventDate.isAfter(alreadyReadingEvent.startDate)) {
                logger.debug { "found ${userBook.readingEvents.count()} older events in CURRENTLY_READING state for book ${userBook.book.id}" }
                alreadyReadingEvent.eventType = createReadingEventDto.eventType
                alreadyReadingEvent.modificationDate = instant
                if (createReadingEventDto.eventType != ReadingEventType.CURRENTLY_READING) {
                    alreadyReadingEvent.endDate = createReadingEventDto.eventDate ?: instant
                }
                // if we  mark book as read, remove to-read flag from the userbook
                if (alreadyReadingEvent.userBook.toRead == true) {
                    alreadyReadingEvent.userBook.toRead = null
                }
                return alreadyReadingEvent
            }
            // FIXME else : alreadyReadingEvent is not null and createReadingEventDto.eventDate is before an already existing CURRENTLY_READING EVENT
            // this could create CURRENTLY_READING events in the past
            // should we allow this ? Let's wait and see for the moment, user can edit events in bookdetail page
        }
        val startDate = computeStartDate(createReadingEventDto, instant)
        val endDate = computeEndDate(createReadingEventDto, instant)
        return ReadingEvent.new {
            this.creationDate = instant
            this.startDate = startDate
            this.endDate = endDate
            this.modificationDate = instant
            this.eventType = createReadingEventDto.eventType
            this.userBook = userBook
        }
    }

    private fun computeEndDate(createReadingEventDto: CreateReadingEventDto, instant: Instant): Instant? {
        return if (createReadingEventDto.eventType != ReadingEventType.CURRENTLY_READING) {
            createReadingEventDto.eventDate ?: instant
        } else {
            null
        }
    }

    private fun computeStartDate(createReadingEventDto: CreateReadingEventDto, fallback: Instant): Instant {
        if (createReadingEventDto.startDate != null) {
            return createReadingEventDto.startDate
        }
        // if eventDate is set we cannot use now as startDate because now could be after eventDate
        // so compute a date just before eventDate if eventDate is set but startdate is not
        if (createReadingEventDto.eventDate != null) {
            val startOfDay = createReadingEventDto.eventDate.truncatedTo(ChronoUnit.DAYS)
            val minusOneHour = createReadingEventDto.eventDate.minus(1, ChronoUnit.HOURS)
            // if removing one hour takes us to the day before, return same day truncated to start of day
            return if (minusOneHour.isBefore(startOfDay)) {
                startOfDay
            } else {
                minusOneHour
            }
        }
        // nothing set, return fallback which is probably now
        return fallback
    }

    fun updateReadingEvent(readingEventId: UUID, updateReadingEventDto: UpdateReadingEventDto): ReadingEvent {
        val entity = ReadingEvent[readingEventId]
        if (updateReadingEventDto.startDate != null &&
            updateReadingEventDto.eventDate != null &&
            updateReadingEventDto.eventDate.isBefore(updateReadingEventDto.startDate)
        ) {
            throw JeluException("start date cannot be after event date")
        } else if (updateReadingEventDto.eventDate != null &&
            updateReadingEventDto.startDate == null &&
            updateReadingEventDto.eventDate.isBefore(entity.startDate)
        ) {
            throw JeluException("event date cannot be before start date")
        } else if (updateReadingEventDto.startDate != null &&
            entity.endDate != null &&
            updateReadingEventDto.eventDate == null &&
            updateReadingEventDto.startDate.isAfter(entity.endDate)
        ) {
            throw JeluException("start date cannot be after end date")
        }
        return entity.apply {
            val instant = nowInstant()
            this.modificationDate = instant
            if (updateReadingEventDto.eventDate != null) {
                this.endDate = updateReadingEventDto.eventDate
            }
            if (updateReadingEventDto.startDate != null) {
                this.startDate = updateReadingEventDto.startDate
            }
            this.eventType = updateReadingEventDto.eventType
            val lastEvent = this.userBook.readingEvents
                .maxByOrNull { e -> e.lastEventDate }
            if (updateReadingEventDto.eventType == ReadingEventType.FINISHED && this.userBook.toRead == true) {
                this.userBook.toRead = null
            }
            if (lastEvent == null) {
                this.userBook.lastReadingEvent = null
                this.userBook.lastReadingEventDate = null
            } else {
                this.userBook.lastReadingEventDate = lastEvent.lastEventDate
                this.userBook.lastReadingEvent = lastEvent.eventType
            }
            // edit an event that was finished or dropped and reset it back to currently reading
            if (updateReadingEventDto.eventType == ReadingEventType.CURRENTLY_READING) {
                this.endDate = null
            }
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
