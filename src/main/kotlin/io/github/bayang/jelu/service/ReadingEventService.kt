package io.github.bayang.jelu.service

import io.github.bayang.jelu.dao.ReadingEventRepository
import io.github.bayang.jelu.dao.ReadingEventType
import io.github.bayang.jelu.dao.User
import io.github.bayang.jelu.dto.CreateReadingEventDto
import io.github.bayang.jelu.dto.ReadingEventDto
import io.github.bayang.jelu.dto.UpdateReadingEventDto
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Component
class ReadingEventService(private val readingEventRepository: ReadingEventRepository) {

    @Transactional
    fun findAll(searchTerm: String?) = readingEventRepository.findAll(searchTerm).map { it.toReadingEventDto() }

    @Transactional
    fun save(createReadingEventDto: CreateReadingEventDto, user: User): ReadingEventDto {
        return readingEventRepository.save(createReadingEventDto, user).toReadingEventDto()
    }

    @Transactional
    fun findAllByUser(userID: UUID): List<ReadingEventDto> = readingEventRepository.findAllByUser(userID).map { it.toReadingEventDto() }

    @Transactional
    fun findAllByUser(user: User, searchTerm: ReadingEventType?): List<ReadingEventDto> = readingEventRepository.findAllByUser(user, searchTerm).map { it.toReadingEventDto() }

    @Transactional
    fun updateReadingEvent(readingEventId: UUID, updateReadingEventDto: UpdateReadingEventDto): ReadingEventDto
    = readingEventRepository.updateReadingEvent(readingEventId, updateReadingEventDto).toReadingEventDto()

}