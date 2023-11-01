package io.github.bayang.jelu.dto

import java.time.Instant

data class LifeCycleDto(
    val id: Long,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val seriesMigrated: Boolean,
)
