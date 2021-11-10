package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dto.CreateReadingEventDto
import io.github.bayang.jelu.dto.ReadingEventDto
import io.github.bayang.jelu.dto.UpdateReadingEventDto
import io.github.bayang.jelu.service.ReadingEventService
import mu.KotlinLogging
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.validation.Valid

private val logger = KotlinLogging.logger {}

@RestController
class ReadingEventsController(
    private val repository: ReadingEventService,
    private val properties: JeluProperties
    ) {

    @GetMapping(path = ["/reading-events"])
    fun readingEvents(): List<ReadingEventDto> = repository.findAll()

    @PostMapping(path = ["/reading-events"])
    fun saveReadingEvent(@RequestBody @Valid event: CreateReadingEventDto): ReadingEventDto {
        return repository.save(event)
    }

    @PutMapping(path = ["/reading-events/{id}"])
    fun updateReadingEvent(@PathVariable("id") readingEventId: UUID, @RequestBody @Valid readingEvent: UpdateReadingEventDto): ReadingEventDto {
        return repository.updateReadingEvent(readingEventId, readingEvent)
    }
}