package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.CreateReadingEventDto
import io.github.bayang.jelu.dto.UpdateReadingEventDto
import io.github.bayang.jelu.utils.nowInstant
import mu.KotlinLogging
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.and
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

private val logger = KotlinLogging.logger {}

@Repository
class ReadingEventRepository(
    private val userRepository: UserRepository,
    private val bookRepository: BookRepository
) {

    fun findAll(searchTerm: String?): SizedIterable<ReadingEvent> {
        return if (! searchTerm.isNullOrBlank()) {
            findAllByUser(UUID.fromString(searchTerm))
        }
        else {
            ReadingEvent.all()
        }
    }

    fun findAllByUser(userID: UUID, searchTerm: ReadingEventType? = null): SizedIterable<ReadingEvent> {
        val user: User = userRepository.findUserById(userID)
        return findAllByUser(user, searchTerm)
    }

    fun findAllByUser(user: User, searchTerm: ReadingEventType?): SizedIterable<ReadingEvent> {
        return if (searchTerm != null) {
            ReadingEvent.find { ReadingEventTable.user eq user.id and (ReadingEventTable.eventType eq searchTerm)}
        } else {
            ReadingEvent.find { ReadingEventTable.user eq user.id }
        }
    }

    fun findByBookUserAndType(user: User, book: Book, eventType: ReadingEventType): SizedIterable<ReadingEvent> {
        return ReadingEvent.find { ReadingEventTable.user eq user.id and (ReadingEventTable.book eq book.id) and (ReadingEventTable.eventType eq eventType) }
    }

    fun save(createReadingEventDto: CreateReadingEventDto, targetUser: User): ReadingEvent {
        val foundBook: Book = bookRepository.findBookById(createReadingEventDto.bookId)
        val events = findByBookUserAndType(targetUser, foundBook, ReadingEventType.CURRENTLY_READING)
        return if (!events.empty()) {
            logger.debug { "found ${events.count()} older events in CURRENTLY_PROCESSING state for book ${foundBook.id}" }
            val oldEvent: ReadingEvent = events.first()
            updateReadingEvent(oldEvent.id.value, UpdateReadingEventDto(createReadingEventDto.eventType))
        } else {
            val created = ReadingEvent.new {
                user = targetUser
                val instant: Instant = nowInstant()
                creationDate = instant
                modificationDate = instant
                book = foundBook
                eventType = createReadingEventDto.eventType
            }
            created
        }
    }

    fun updateReadingEvent(readingEventId: UUID, updateReadingEventDto: UpdateReadingEventDto): ReadingEvent {
        return ReadingEvent[readingEventId].apply {
            this.modificationDate = nowInstant()
            this.eventType = updateReadingEventDto.eventType
        }
    }

}