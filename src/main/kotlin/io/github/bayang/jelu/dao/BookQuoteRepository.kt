package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.CreateBookQuoteDto
import io.github.bayang.jelu.dto.UpdateBookQuoteDto
import io.github.bayang.jelu.dto.UserDto
import io.github.bayang.jelu.utils.nowInstant
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
class BookQuoteRepository {

    fun findById(quoteId: UUID): BookQuote = BookQuote[quoteId]

    fun save(bookQuoteDto: CreateBookQuoteDto, user: UserDto): BookQuote {
        val instant: Instant = nowInstant()
        return BookQuote.new {
            this.creationDate = instant
            this.modificationDate = instant
            this.text = bookQuoteDto.text
            this.visibility = bookQuoteDto.visibility
            this.book = Book[bookQuoteDto.bookId]
            this.user = User[user.id!!]
            this.position = bookQuoteDto.position
        }
    }

    fun find(
        userId: UUID?,
        bookId: UUID?,
        visibility: Visibility?,
        pageable: Pageable,
    ): Page<BookQuote> {
        val query = BookQuoteTable.selectAll()
        if (userId != null) {
            query.andWhere { BookQuoteTable.user eq userId }
        }
        if (bookId != null) {
            query.andWhere { BookQuoteTable.book eq bookId }
        }
        if (visibility != null) {
            query.andWhere { BookQuoteTable.visibility eq visibility }
        }
        val total = query.count()
        query.limit(pageable.pageSize, pageable.offset)
        val orders: Array<Pair<Expression<*>, SortOrder>> =
            parseSorts(pageable.sort, Pair(BookQuoteTable.creationDate, SortOrder.DESC_NULLS_LAST), BookQuoteTable)
        query.orderBy(*orders)
        return PageImpl(
            BookQuote.wrapRows(query).toList(),
            pageable,
            total,
        )
    }

    fun update(bookQuoteId: UUID, updateBookQuoteDto: UpdateBookQuoteDto): BookQuote {
        return BookQuote[bookQuoteId].apply {
            this.modificationDate = nowInstant()
            if (updateBookQuoteDto.position != null) {
                this.position = updateBookQuoteDto.position
            }
            if (updateBookQuoteDto.text != null) {
                this.text = updateBookQuoteDto.text
            }
            if (updateBookQuoteDto.visibility != null) {
                this.visibility = updateBookQuoteDto.visibility
            }
        }
    }

    fun delete(bookQuoteId: UUID) {
        BookQuote[bookQuoteId].delete()
    }
}
