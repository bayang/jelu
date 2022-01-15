package io.github.bayang.jelu.service

import io.github.bayang.jelu.dao.ReadingEventRepository
import io.github.bayang.jelu.dao.ReadingEventType
import io.github.bayang.jelu.dao.User
import io.github.bayang.jelu.dto.CreateReadingEventDto
import io.github.bayang.jelu.dto.ReadingEventDto
import io.github.bayang.jelu.dto.UpdateReadingEventDto
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Component
class ReadingEventService(private val readingEventRepository: ReadingEventRepository) {

    @Transactional
    fun findAll(searchTerm: ReadingEventType?, userId: UUID?, page: Long = 0, pageSize: Long = 20) =
        readingEventRepository.findAll(searchTerm, userId, page, pageSize).map { it.toReadingEventDto() }

    @Transactional
    fun save(createReadingEventDto: CreateReadingEventDto, user: User): ReadingEventDto {
        return readingEventRepository.save(createReadingEventDto, user).toReadingEventDto()
    }

    @Transactional
    fun findAllByUser(user: User, searchTerm: ReadingEventType?, page: Long, pageSize: Long): Page<ReadingEventDto> =
        readingEventRepository.findAllByUser(user.id.value, searchTerm, page, pageSize).map { it.toReadingEventDto() }

    @Transactional
    fun updateReadingEvent(readingEventId: UUID, updateReadingEventDto: UpdateReadingEventDto): ReadingEventDto =
        readingEventRepository.updateReadingEvent(readingEventId, updateReadingEventDto).toReadingEventDto()

    @Transactional
    fun deleteReadingEventById(eventId: UUID) {
        readingEventRepository.deleteReadingEventById(eventId)
    }
}
