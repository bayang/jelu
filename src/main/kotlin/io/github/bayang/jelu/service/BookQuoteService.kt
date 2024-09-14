package io.github.bayang.jelu.service

import io.github.bayang.jelu.dao.BookQuoteRepository
import io.github.bayang.jelu.dao.User
import io.github.bayang.jelu.dao.Visibility
import io.github.bayang.jelu.dto.BookQuoteDto
import io.github.bayang.jelu.dto.CreateBookQuoteDto
import io.github.bayang.jelu.dto.UpdateBookQuoteDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class BookQuoteService(
    private val bookQuoteRepository: BookQuoteRepository,
) {

    @Transactional
    fun save(bookQuoteDto: CreateBookQuoteDto, user: User): BookQuoteDto {
        return bookQuoteRepository.save(bookQuoteDto, user).toBookQuoteDto()
    }

    @Transactional
    fun findById(bookQuoteId: UUID): BookQuoteDto = bookQuoteRepository.findById(bookQuoteId).toBookQuoteDto()

    @Transactional
    fun find(
        userId: UUID?,
        bookId: UUID?,
        visibility: Visibility?,
        pageable: Pageable,
    ): Page<BookQuoteDto> {
        return bookQuoteRepository.find(userId, bookId, visibility, pageable).map { it.toBookQuoteDto() }
    }

    @Transactional
    fun update(bookQuoteId: UUID, updateBookQuoteDto: UpdateBookQuoteDto): BookQuoteDto {
        return bookQuoteRepository.update(bookQuoteId, updateBookQuoteDto).toBookQuoteDto()
    }

    @Transactional
    fun delete(bookQuoteId: UUID) = bookQuoteRepository.delete(bookQuoteId)
}
