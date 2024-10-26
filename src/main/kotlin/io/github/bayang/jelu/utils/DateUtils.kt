package io.github.bayang.jelu.utils

import io.github.bayang.jelu.dto.ReadingEventDto
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun nowInstant(): Instant = OffsetDateTime.now(ZoneId.systemDefault()).toInstant()

fun toInstant(date: LocalDate): Instant = date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()

fun lastEventDate(dto: ReadingEventDto): Instant? {
    return dto.endDate ?: dto.startDate
}

fun stringFormat(instant: Instant?): String {
    if (instant == null) {
        return ""
    }
    return instant.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_DATE_TIME)
}
