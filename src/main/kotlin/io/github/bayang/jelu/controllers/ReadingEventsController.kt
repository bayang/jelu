package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dao.ReadingEventType
import io.github.bayang.jelu.dto.*
import io.github.bayang.jelu.service.ReadingEventService
import mu.KotlinLogging
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.validation.Valid

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api")
class ReadingEventsController(
    private val repository: ReadingEventService,
    private val properties: JeluProperties
    ) {

    @GetMapping(path = ["/reading-events"])
    fun readingEvents(@RequestParam(name = "q", required = false) searchTerm: String?): List<ReadingEventDto> = repository.findAll(searchTerm)

    @GetMapping(path = ["/reading-events/me"])
    fun myReadingEvents(@RequestParam(name = "type", required = false) searchTerm: ReadingEventType?, principal: Authentication): List<ReadingEventDto> {
        assertIsJeluUser(principal.principal)
        return repository.findAllByUser((principal.principal as JeluUser).user, searchTerm)
    }

    @PostMapping(path = ["/reading-events"])
    fun saveReadingEvent(@RequestBody @Valid event: CreateReadingEventDto, principal: Authentication): ReadingEventDto {
        logger.debug { "event creation request for ${principal.name}" }
        assertIsJeluUser(principal.principal)
        return repository.save(event, (principal.principal as JeluUser).user)
    }

    @PutMapping(path = ["/reading-events/{id}"])
    fun updateReadingEvent(@PathVariable("id") readingEventId: UUID, @RequestBody @Valid readingEvent: UpdateReadingEventDto): ReadingEventDto {
        return repository.updateReadingEvent(readingEventId, readingEvent)
    }
}