package io.github.bayang.jelu.dto

import io.github.bayang.jelu.dao.Visibility
import java.time.Instant
import java.util.UUID

data class ReviewDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val reviewDate: Instant?,
    val text: String,
    val rating: Double,
    val visibility: Visibility,
    val user: UUID?,
    val book: UUID?
)
data class UpdateReviewDto(
    val reviewDate: Instant?,
    val text: String?,
    val rating: Double?,
    val visibility: Visibility?
)
data class CreateReviewDto(
    val reviewDate: Instant?,
    val text: String,
    val rating: Double,
    val visibility: Visibility,
    val bookId: UUID
)
