package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.CreateReadingEventDto
import io.github.bayang.jelu.dto.UpdateReadingEventDto
import io.github.bayang.jelu.utils.nowInstant
import mu.KotlinLogging
import org.jetbrains.exposed.sql.and
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

private val logger = KotlinLogging.logger {}

@Repository
class ReadingEventRepository(
    private val bookRepository: BookRepository
) {

    fun findAll(searchTerm: String?): List<ReadingEvent> {
        return if (! searchTerm.isNullOrBlank()) {
            findAllByUser(UUID.fromString(searchTerm))
        }
        else {
            ReadingEvent.all().toList()
        }
    }

    fun findAllByUser(userID: UUID, searchTerm: ReadingEventType? = null): List<ReadingEvent> {
        return UserBook.find { UserBookTable.user eq userID }
            .flatMap { it.readingEvents }
            .filter {
                if (searchTerm != null) {
                    it.eventType == searchTerm
                }
                else {
                    true
                }
             }
    }

//    fun findAllByUser(user: User, searchTerm: ReadingEventType?): SizedIterable<ReadingEvent> {
//        return if (searchTerm != null) {
//            ReadingEvent.find { ReadingEventTable.user eq user.id and (ReadingEventTable.eventType eq searchTerm)}
//        } else {
//            ReadingEvent.find { ReadingEventTable.user eq user.id }
//        }
//    }

//    fun findByBookUserAndType(user: User, book: Book, eventType: ReadingEventType): SizedIterable<ReadingEvent> {
//        return ReadingEvent.find { ReadingEventTable.user eq user.id and (ReadingEventTable.book eq book.id) and (ReadingEventTable.eventType eq eventType) }
//    }

    fun save(createReadingEventDto: CreateReadingEventDto, targetUser: User): ReadingEvent {
        val foundBook: Book = bookRepository.findBookById(createReadingEventDto.bookId)
        return this.save(createReadingEventDto, foundBook, targetUser)
    }

    fun save(createReadingEventDto: CreateReadingEventDto, book: Book, targetUser: User): ReadingEvent {
        var found: UserBook? = UserBook.find { UserBookTable.user eq targetUser.id and (UserBookTable.book.eq(book.id)) }.firstOrNull()
        val instant: Instant = nowInstant()
        if (found == null) {
            found = UserBook.new {
                this.creationDate = instant
                this.user = targetUser
                this.book = book
            }
        }
        found.modificationDate = instant
        val alreadyReadingEvent: ReadingEvent? = found.readingEvents.find { it.eventType == ReadingEventType.CURRENTLY_READING }
        if (alreadyReadingEvent != null) {
            logger.debug { "found ${found.readingEvents.count()} older events in CURRENTLY_PROCESSING state for book ${book.id}" }
            alreadyReadingEvent.eventType = createReadingEventDto.eventType
            alreadyReadingEvent.modificationDate = instant
            return alreadyReadingEvent
        }
        return ReadingEvent.new {
            val instant: Instant = nowInstant()
            this.creationDate = instant
            this.modificationDate = instant
            this.eventType = createReadingEventDto.eventType
            this.userBook = found
        }
    }

    fun updateReadingEvent(readingEventId: UUID, updateReadingEventDto: UpdateReadingEventDto): ReadingEvent {
        return ReadingEvent[readingEventId].apply {
            this.modificationDate = nowInstant()
            this.eventType = updateReadingEventDto.eventType
        }
    }

}