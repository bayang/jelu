package io.github.bayang.jelu.service

import io.github.bayang.jelu.dao.ReadingEventRepository
import io.github.bayang.jelu.dto.CreateReadingEventDto
import io.github.bayang.jelu.dto.ReadingEventDto
import io.github.bayang.jelu.dto.UpdateReadingEventDto
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Component
class ReadingEventService(private val readingEventRepository: ReadingEventRepository) {

    @Transactional
    fun findAll() = readingEventRepository.findAll().map { it.toReadingEventDto() }

    @Transactional
    fun save(createReadingEventDto: CreateReadingEventDto): ReadingEventDto =
        readingEventRepository.save(createReadingEventDto).toReadingEventDto()

    @Transactional
    fun updateReadingEvent(readingEventId: UUID, updateReadingEventDto: UpdateReadingEventDto): ReadingEventDto
    = readingEventRepository.updateReadingEvent(readingEventId, updateReadingEventDto).toReadingEventDto()
}