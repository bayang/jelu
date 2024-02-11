package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dao.ReadingEventType
import io.github.bayang.jelu.dto.CreateReadingEventDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.dto.MonthStatsDto
import io.github.bayang.jelu.dto.ReadingEventDto
import io.github.bayang.jelu.dto.UpdateReadingEventDto
import io.github.bayang.jelu.dto.YearStatsDto
import io.github.bayang.jelu.dto.assertIsJeluUser
import io.github.bayang.jelu.service.ReadingEventService
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import mu.KotlinLogging
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
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
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1")
class ReadingEventsController(
    private val repository: ReadingEventService,
    private val properties: JeluProperties,
) {

    @GetMapping(path = ["/reading-events"])
    fun readingEvents(
        @RequestParam(name = "eventTypes", required = false) eventTypes: List<ReadingEventType>?,
        @RequestParam(name = "userId", required = false) userId: UUID?,
        @RequestParam(name = "bookId", required = false) bookId: UUID?,
        @RequestParam(name = "startedAfter", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        startedAfter: LocalDate?,
        @RequestParam(name = "startedBefore", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        startedBefore: LocalDate?,
        @RequestParam(name = "endedAfter", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        endedAfter: LocalDate?,
        @RequestParam(name = "endedBefore", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        endedBefore: LocalDate?,
        @PageableDefault(page = 0, size = 20, direction = Sort.Direction.DESC, sort = ["modificationDate"]) @ParameterObject pageable: Pageable,
    ): Page<ReadingEventDto> = repository.findAll(eventTypes, userId, bookId, startedAfter, startedBefore, endedAfter, endedBefore, pageable)

    @GetMapping(path = ["/reading-events/me"])
    fun myReadingEvents(
        @RequestParam(name = "eventTypes", required = false) eventTypes: List<ReadingEventType>?,
        @RequestParam(name = "bookId", required = false) bookId: UUID?,
        @RequestParam(name = "startedAfter", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        startedAfter: LocalDate?,
        @RequestParam(name = "startedBefore", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        startedBefore: LocalDate?,
        @RequestParam(name = "endedAfter", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        endedAfter: LocalDate?,
        @RequestParam(name = "endedBefore", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        endedBefore: LocalDate?,
        @PageableDefault(page = 0, size = 20, direction = Sort.Direction.DESC, sort = ["modificationDate"]) @ParameterObject pageable: Pageable,
        principal: Authentication,
    ): Page<ReadingEventDto> {
        assertIsJeluUser(principal.principal)
        return repository.findAll(eventTypes, (principal.principal as JeluUser).user.id.value, bookId, startedAfter, startedBefore, endedAfter, endedBefore, pageable)
    }

    @PostMapping(path = ["/reading-events"])
    fun saveReadingEvent(
        @RequestBody @Valid
        event: CreateReadingEventDto,
        principal: Authentication,
    ): ReadingEventDto {
        logger.debug { "event creation request for ${principal.name}" }
        assertIsJeluUser(principal.principal)
        return repository.save(event, (principal.principal as JeluUser).user)
    }

    @PutMapping(path = ["/reading-events/{id}"])
    fun updateReadingEvent(
        @PathVariable("id")
        readingEventId: UUID,
        @RequestBody
        @Valid
        readingEvent: UpdateReadingEventDto,
    ): ReadingEventDto {
        return repository.updateReadingEvent(readingEventId, readingEvent)
    }

    @ApiResponse(responseCode = "204", description = "Deleted the reading event")
    @DeleteMapping(path = ["/reading-events/{id}"])
    fun deleteEventById(
        @PathVariable("id")
        eventId: UUID,
    ): ResponseEntity<Unit> {
        repository.deleteReadingEventById(eventId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping(path = ["/stats"])
    fun stats(
        principal: Authentication,
    ): ResponseEntity<List<YearStatsDto>> {
        var events: Page<ReadingEventDto>
        var currentPage = 0
        val pageSize = 200
        val yearStats = mutableMapOf<Int, YearStatsDto>()
        do {
            events = repository.findAll(listOf(ReadingEventType.FINISHED, ReadingEventType.DROPPED), (principal.principal as JeluUser).user.id.value, null, null, null, null, null, PageRequest.of(currentPage, pageSize, Sort.by("endDate, asc")))
            currentPage++
            events.forEach {
                val year = OffsetDateTime.ofInstant(it.endDate, ZoneId.systemDefault()).year
                if (yearStats.containsKey(year)) {
                    if (it.eventType == ReadingEventType.DROPPED) {
                        yearStats[year] = yearStats[year]!!.copy(dropped = yearStats[year]!!.dropped + 1)
                    } else if (it.eventType == ReadingEventType.FINISHED) {
                        yearStats[year] = yearStats[year]!!.copy(finished = yearStats[year]!!.finished + 1, pageCount = yearStats[year]!!.pageCount + (it.userBook.book.pageCount ?: 0))
                    }
                } else {
                    if (it.eventType == ReadingEventType.DROPPED) {
                        yearStats[year] = YearStatsDto(year = year, dropped = 1)
                    } else if (it.eventType == ReadingEventType.FINISHED) {
                        yearStats[year] = YearStatsDto(year = year, finished = 1, pageCount = it.userBook.book.pageCount ?: 0)
                    }
                }
            }
        } while (!events.isEmpty)
        return ResponseEntity.ok(yearStats.values.sortedBy { it.year })
    }

    @GetMapping(path = ["/stats/{year}"])
    fun statsForYear(
        @PathVariable("year") year: Int,
        principal: Authentication,
    ): ResponseEntity<List<MonthStatsDto>> {
        var events: Page<ReadingEventDto>
        var currentPage = 0
        val pageSize = 200
        val monthStats = mutableMapOf<Int, MonthStatsDto>()
        do {
            events = repository.findAll(listOf(ReadingEventType.FINISHED, ReadingEventType.DROPPED), (principal.principal as JeluUser).user.id.value, null, null, null, null, null, PageRequest.of(currentPage, pageSize))
            currentPage++
            // FIXME use date filtering in repository method now
            events.filter { OffsetDateTime.ofInstant(it.endDate, ZoneId.systemDefault()).year == year }.forEach {
                val toDate = OffsetDateTime.ofInstant(it.endDate, ZoneId.systemDefault())
                val month = toDate.monthValue
                if (monthStats.containsKey(month)) {
                    if (it.eventType == ReadingEventType.DROPPED) {
                        monthStats[month] = monthStats[month]!!.copy(dropped = monthStats[month]!!.dropped + 1)
                    } else if (it.eventType == ReadingEventType.FINISHED) {
                        monthStats[month] = monthStats[month]!!.copy(finished = monthStats[month]!!.finished + 1, pageCount = monthStats[month]!!.pageCount + (it.userBook.book.pageCount ?: 0))
                    }
                } else {
                    if (it.eventType == ReadingEventType.DROPPED) {
                        monthStats[month] = MonthStatsDto(year = year, dropped = 1, month = month)
                    } else if (it.eventType == ReadingEventType.FINISHED) {
                        monthStats[month] = MonthStatsDto(year = year, finished = 1, month = month, pageCount = it.userBook.book.pageCount ?: 0)
                    }
                }
            }
        } while (!events.isEmpty)
        return ResponseEntity.ok(monthStats.values.sortedBy { it.month })
    }

    @ApiResponse(description = "Return a list of years for which there are reading events")
    @GetMapping(path = ["/stats/years"])
    fun years(
        principal: Authentication,
    ): ResponseEntity<List<Int>> {
        val years = repository.findYears(listOf(ReadingEventType.FINISHED, ReadingEventType.DROPPED), (principal.principal as JeluUser).user.id.value, null)
        return if (years != null && years.isNotEmpty()) {
            ResponseEntity.ok(years.sorted())
        } else {
            ResponseEntity.ok(emptyList())
        }
    }
}
