package io.github.bayang.jelu.utils

import io.github.bayang.jelu.dto.ReadingEventDto
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun nowInstant(): Instant = OffsetDateTime.now(ZoneId.systemDefault()).toInstant()

fun nowDateTime(): OffsetDateTime = OffsetDateTime.now(ZoneId.systemDefault())

fun offset(): ZoneOffset {
    val now = LocalDateTime.now()
    val zone = ZoneId.systemDefault()
    return zone.rules.getOffset(now)
}

fun toOffsetDatetime(date: LocalDate): OffsetDateTime = date.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime()

fun lastEventDate(dto: ReadingEventDto): OffsetDateTime? = dto.endDate ?: dto.startDate

fun stringFormat(instant: Instant?): String {
    if (instant == null) {
        return ""
    }
    return instant.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_DATE_TIME)
}
