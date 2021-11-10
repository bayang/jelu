package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.CreateReadingEventDto
import io.github.bayang.jelu.dto.UpdateReadingEventDto
import io.github.bayang.jelu.utils.nowInstant
import mu.KotlinLogging
import org.jetbrains.exposed.sql.SizedIterable
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

private val logger = KotlinLogging.logger {}

@Repository
class ReadingEventRepository(
    private val userRepository: UserRepository,
    private val bookRepository: BookRepository
) {

    fun findAll(): SizedIterable<ReadingEvent> = ReadingEvent.all()

    fun save(createReadingEventDto: CreateReadingEventDto): ReadingEvent {
        var foundUser: User = userRepository.findUserById(createReadingEventDto.userId)
        val foundBook: Book = bookRepository.findBookById(createReadingEventDto.bookId)
        val created = ReadingEvent.new{
            user = foundUser
            val instant: Instant = nowInstant()
            creationDate = instant
            modificationDate = instant
            book = foundBook
            eventType = createReadingEventDto.eventType
        }
        return created
    }

    fun updateReadingEvent(readingEventId: UUID, updateReadingEventDto: UpdateReadingEventDto): ReadingEvent {
        return ReadingEvent[readingEventId].apply {
            this.modificationDate = nowInstant()
            this.eventType = updateReadingEventDto.eventType
        }
    }

}