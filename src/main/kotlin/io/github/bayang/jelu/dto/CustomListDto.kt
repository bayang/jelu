package io.github.bayang.jelu.dto

import java.time.Instant
import java.util.UUID

data class CustomListDto(
    val id: UUID?,
    val name: String,
    val tags: String,
    val public: Boolean,
    val actionable: Boolean,
    val creationDate: Instant?,
    val modificationDate: Instant?,
)

data class CustomListRemoveDto(
    val books: List<String>,
    val tags: List<String>,
)
