package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.CreateReviewDto
import io.github.bayang.jelu.dto.UpdateReviewDto
import io.github.bayang.jelu.dto.UserDto
import io.github.bayang.jelu.utils.nowInstant
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Repository
class ReviewRepository {

    fun findById(reviewId: UUID): Review = Review[reviewId]

    fun save(reviewDto: CreateReviewDto, user: UserDto): Review {
        val instant: Instant = nowInstant()
        return Review.new {
            this.creationDate = instant
            this.modificationDate = instant
            if (reviewDto.reviewDate != null) {
                this.reviewDate = reviewDto.reviewDate
            } else {
                this.reviewDate = instant
            }
            this.rating = reviewDto.rating
            this.text = reviewDto.text
            this.visibility = reviewDto.visibility
            this.book = Book[reviewDto.bookId]
            this.user = User[user.id!!]
        }
    }

    fun find(
        userId: UUID?,
        bookId: UUID?,
        visibility: Visibility?,
        after: LocalDate?,
        before: LocalDate?,
        pageable: Pageable,
    ): Page<Review> {
        val query = ReviewTable.selectAll()
        if (userId != null) {
            query.andWhere { ReviewTable.user eq userId }
        }
        if (bookId != null) {
            query.andWhere { ReviewTable.book eq bookId }
        }
        if (visibility != null) {
            query.andWhere { ReviewTable.visibility eq visibility }
        }
        if (before != null) {
            val instant = OffsetDateTime.of(before, LocalTime.MAX, ZoneId.systemDefault().rules.getOffset(nowInstant())).toInstant()
            query.andWhere { ReviewTable.reviewDate lessEq instant }
        }
        if (after != null) {
            val instant = OffsetDateTime.of(after, LocalTime.MIN, ZoneId.systemDefault().rules.getOffset(nowInstant())).toInstant()
            query.andWhere { ReviewTable.reviewDate greaterEq instant }
        }
        val total = query.count()
        query.limit(pageable.pageSize, pageable.offset)
        val orders: Array<Pair<Expression<*>, SortOrder>> = parseSorts(pageable.sort, Pair(ReviewTable.reviewDate, SortOrder.DESC_NULLS_LAST), ReviewTable)
        query.orderBy(*orders)
        return PageImpl(
            Review.wrapRows(query).toList(),
            pageable,
            total,
        )
    }

    fun update(reviewId: UUID, updateReviewDto: UpdateReviewDto): Review {
        return Review[reviewId].apply {
            this.modificationDate = nowInstant()
            if (updateReviewDto.reviewDate != null) {
                this.reviewDate = updateReviewDto.reviewDate
            }
            if (updateReviewDto.rating != null) {
                this.rating = updateReviewDto.rating
            }
            if (updateReviewDto.text != null) {
                this.text = updateReviewDto.text
            }
            if (updateReviewDto.visibility != null) {
                this.visibility = updateReviewDto.visibility
            }
        }
    }

    fun delete(reviewId: UUID) {
        Review[reviewId].delete()
    }
}
