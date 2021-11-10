package io.github.bayang.jelu.utils

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

fun nowInstant(): Instant = OffsetDateTime.now(ZoneId.systemDefault()).toInstant()
