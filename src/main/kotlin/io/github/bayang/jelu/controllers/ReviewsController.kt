package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.dao.Visibility
import io.github.bayang.jelu.dto.CreateReviewDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.dto.ReviewDto
import io.github.bayang.jelu.dto.UpdateReviewDto
import io.github.bayang.jelu.errors.JeluAuthenticationException
import io.github.bayang.jelu.service.ReviewService
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class ReviewsController(
    private val reviewService: ReviewService,
) {

    @PostMapping(path = ["/reviews"])
    fun createReview(
        @RequestBody @Valid
        createReviewDto: CreateReviewDto,
        principal: Authentication,
    ): ReviewDto {
        return reviewService.save(createReviewDto, (principal.principal as JeluUser).user)
    }

    @GetMapping(path = ["/reviews"])
    fun reviews(
        @RequestParam(name = "userId", required = false) userId: UUID?,
        @RequestParam(name = "bookId", required = false) bookId: UUID?,
        @RequestParam(name = "visibility", required = false) visibility: Visibility?,
        @RequestParam(name = "after", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        after: LocalDate?,
        @RequestParam(name = "before", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        before: LocalDate?,
        @PageableDefault(page = 0, size = 20, direction = Sort.Direction.DESC, sort = ["reviewDate"]) @ParameterObject pageable: Pageable,
    ): Page<ReviewDto> {
        return reviewService.find(userId, bookId, visibility, after, before, pageable)
    }

    @PutMapping(path = ["/reviews/{id}"])
    fun updateReview(
        @PathVariable("id") reviewId: UUID,
        @RequestBody @Valid
        updateReviewDto: UpdateReviewDto,
    ): ReviewDto {
        return reviewService.update(reviewId, updateReviewDto)
    }

    @GetMapping(path = ["/reviews/{id}"])
    fun getReview(
        @PathVariable("id") reviewId: UUID,
        principal: Authentication?,
    ): ReviewDto {
        val review = reviewService.findById(reviewId)
        if (review.visibility == Visibility.PRIVATE && principal == null) {
            throw JeluAuthenticationException("Resource unauthorized")
        }
        return review
    }

    @ApiResponse(responseCode = "204", description = "Deleted the review")
    @DeleteMapping(path = ["/reviews/{id}"])
    fun deleteReviewById(@PathVariable("id") reviewId: UUID): ResponseEntity<Unit> {
        reviewService.delete(reviewId)
        return ResponseEntity.noContent().build()
    }
}
