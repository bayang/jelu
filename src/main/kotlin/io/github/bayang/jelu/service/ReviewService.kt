package io.github.bayang.jelu.service

import io.github.bayang.jelu.dao.ReviewRepository
import io.github.bayang.jelu.dao.Visibility
import io.github.bayang.jelu.dto.CreateReviewDto
import io.github.bayang.jelu.dto.ReviewDto
import io.github.bayang.jelu.dto.UpdateReviewDto
import io.github.bayang.jelu.dto.UserDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Component
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val bookService: BookService,
) {

    @Transactional
    fun save(reviewDto: CreateReviewDto, user: UserDto): ReviewDto {
        return reviewRepository.save(reviewDto, user).toReviewDto()
    }

    @Transactional
    fun findById(reviewId: UUID): ReviewDto = reviewRepository.findById(reviewId).toReviewDto()

    @Transactional
    fun find(
        userId: UUID?,
        bookId: UUID?,
        visibility: Visibility?,
        after: LocalDate?,
        before: LocalDate?,
        pageable: Pageable,
    ): Page<ReviewDto> {
        return reviewRepository.find(userId, bookId, visibility, after, before, pageable).map { it.toReviewDto() }
    }

    @Transactional
    fun update(reviewId: UUID, updateReviewDto: UpdateReviewDto): ReviewDto {
        return reviewRepository.update(reviewId, updateReviewDto).toReviewDto()
    }

    @Transactional
    fun delete(reviewId: UUID) = reviewRepository.delete(reviewId)
}
