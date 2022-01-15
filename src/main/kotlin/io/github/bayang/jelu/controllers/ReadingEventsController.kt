package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dao.ReadingEventType
import io.github.bayang.jelu.dto.*
import io.github.bayang.jelu.service.ReadingEventService
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
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
    fun readingEvents(
        @RequestParam(name = "type", required = false) searchTerm: ReadingEventType?,
        @RequestParam(name = "userId", required = false) userId: UUID?,
        @RequestParam(name = "page", required = false, defaultValue = "0") page: Long,
        @RequestParam(name = "pageSize", required = false, defaultValue = "20") pageSize: Long
    ): Page<ReadingEventDto> = repository.findAll(searchTerm, userId, page, pageSize)

    @GetMapping(path = ["/reading-events/me"])
    fun myReadingEvents(
        @RequestParam(name = "type", required = false) searchTerm: ReadingEventType?,
        @RequestParam(name = "page", required = false, defaultValue = "0") page: Long,
        @RequestParam(name = "pageSize", required = false, defaultValue = "20") pageSize: Long,
        principal: Authentication
    ): Page<ReadingEventDto> {
        assertIsJeluUser(principal.principal)
        return repository.findAllByUser((principal.principal as JeluUser).user, searchTerm, page, pageSize)
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

    @DeleteMapping(path = ["/reading-events/{id}"])
    fun deleteEventById(@PathVariable("id") eventId: UUID): ResponseEntity<Unit> {
        repository.deleteReadingEventById(eventId)
        return ResponseEntity.noContent().build()
    }
}
