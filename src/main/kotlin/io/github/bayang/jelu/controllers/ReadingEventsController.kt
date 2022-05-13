package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dao.ReadingEventType
import io.github.bayang.jelu.dto.CreateReadingEventDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.dto.ReadingEventDto
import io.github.bayang.jelu.dto.UpdateReadingEventDto
import io.github.bayang.jelu.dto.assertIsJeluUser
import io.github.bayang.jelu.service.ReadingEventService
import io.swagger.v3.oas.annotations.responses.ApiResponse
import mu.KotlinLogging
import org.springdoc.api.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Valid

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1")
class ReadingEventsController(
    private val repository: ReadingEventService,
    private val properties: JeluProperties
) {

    @GetMapping(path = ["/reading-events"])
    fun readingEvents(
        @RequestParam(name = "eventTypes", required = false) eventTypes: List<ReadingEventType>?,
        @RequestParam(name = "userId", required = false) userId: UUID?,
        @RequestParam(name = "bookId", required = false) bookId: UUID?,
        @PageableDefault(page = 0, size = 20, direction = Sort.Direction.DESC, sort = ["modificationDate"]) @ParameterObject pageable: Pageable
    ): Page<ReadingEventDto> = repository.findAll(eventTypes, userId, bookId, pageable)

    @GetMapping(path = ["/reading-events/me"])
    fun myReadingEvents(
        @RequestParam(name = "eventTypes", required = false) eventTypes: List<ReadingEventType>?,
        @PageableDefault(page = 0, size = 20, direction = Sort.Direction.DESC, sort = ["modificationDate"]) @ParameterObject pageable: Pageable,
        principal: Authentication
    ): Page<ReadingEventDto> {
        assertIsJeluUser(principal.principal)
        return repository.findAll(eventTypes, (principal.principal as JeluUser).user.id.value, null, pageable)
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

    @ApiResponse(responseCode = "204", description = "Deleted the reading event")
    @DeleteMapping(path = ["/reading-events/{id}"])
    fun deleteEventById(@PathVariable("id") eventId: UUID): ResponseEntity<Unit> {
        repository.deleteReadingEventById(eventId)
        return ResponseEntity.noContent().build()
    }
}
