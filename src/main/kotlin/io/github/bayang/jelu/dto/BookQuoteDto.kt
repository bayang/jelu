package io.github.bayang.jelu.dto

import io.github.bayang.jelu.dao.Visibility
import java.time.Instant
import java.util.UUID

data class BookQuoteDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val text: String,
    val visibility: Visibility,
    val user: UUID?,
    val book: UUID?,
    val position: String?,
)
data class UpdateBookQuoteDto(
    val text: String?,
    val visibility: Visibility?,
    val position: String?,
)
data class CreateBookQuoteDto(
    val text: String,
    val visibility: Visibility,
    val bookId: UUID,
    val position: String?,
)
