package io.github.bayang.jelu.dto

import java.time.Instant
import java.util.UUID

data class ShelfDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val name: String,
    val targetId: UUID
)
data class CreateShelfDto(
    val name: String,
    val targetId: UUID
)
