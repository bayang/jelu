package io.github.bayang.jelu.service

import io.github.bayang.jelu.dao.ReadingEventRepository
import io.github.bayang.jelu.dao.ReadingEventType
import io.github.bayang.jelu.dto.CreateReadingEventDto
import io.github.bayang.jelu.dto.ReadingEventDto
import io.github.bayang.jelu.dto.UpdateReadingEventDto
import io.github.bayang.jelu.dto.UserDto
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Component
class ReadingEventService(private val readingEventRepository: ReadingEventRepository) {

    @Transactional
    fun findAll(
        eventTypes: List<ReadingEventType>?,
        userId: UUID?,
        bookId: UUID?,
        startedAfter: LocalDate?,
        startedBefore: LocalDate?,
        endedAfter: LocalDate?,
        endedBefore: LocalDate?,
        pageable: Pageable,
    ) =
        readingEventRepository.findAll(eventTypes, userId, bookId, startedAfter, startedBefore, endedAfter, endedBefore, pageable).map { it.toReadingEventDto() }

    @Transactional
    fun findYears(eventTypes: List<ReadingEventType>?, userId: UUID?, bookId: UUID?) =
        readingEventRepository.findYears(eventTypes, userId, bookId)

    @Transactional
    fun save(createReadingEventDto: CreateReadingEventDto, user: UserDto): ReadingEventDto {
        return readingEventRepository.save(createReadingEventDto, user).toReadingEventDto()
    }

    @Transactional
    fun updateReadingEvent(readingEventId: UUID, updateReadingEventDto: UpdateReadingEventDto): ReadingEventDto =
        readingEventRepository.updateReadingEvent(readingEventId, updateReadingEventDto).toReadingEventDto()

    @Transactional
    fun deleteReadingEventById(eventId: UUID) {
        readingEventRepository.deleteReadingEventById(eventId)
    }
}
