package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.CreateReadingEventDto
import io.github.bayang.jelu.dto.UpdateReadingEventDto
import io.github.bayang.jelu.errors.JeluException
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
        if (createReadingEventDto.bookId == null) {
            throw JeluException("Missing bookId to create reading event")
        }
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
        return save(found, createReadingEventDto)
    }

    fun save(userBook: UserBook, createReadingEventDto: CreateReadingEventDto): ReadingEvent {
        val alreadyReadingEvent: ReadingEvent? = userBook.readingEvents.find { it.eventType == ReadingEventType.CURRENTLY_READING }
        userBook.lastReadingEvent = createReadingEventDto.eventType
        val instant: Instant = nowInstant()
        if (alreadyReadingEvent != null) {
            logger.debug { "found ${userBook.readingEvents.count()} older events in CURRENTLY_PROCESSING state for book ${userBook.book.id}" }
            alreadyReadingEvent.eventType = createReadingEventDto.eventType
            alreadyReadingEvent.modificationDate = instant
            return alreadyReadingEvent
        }
        return ReadingEvent.new {
            this.creationDate = instant
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
        }
    }

}