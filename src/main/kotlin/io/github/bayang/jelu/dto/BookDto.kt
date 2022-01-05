package io.github.bayang.jelu.dto

import io.github.bayang.jelu.dao.BookTable
import io.github.bayang.jelu.dao.BookTable.nullable
import io.github.bayang.jelu.dao.ReadingEventType
import org.jetbrains.exposed.sql.Column
import java.time.Instant
import java.util.*

data class BookDto(
    val id: UUID?,
    val creationDate: Instant?,
    val title: String,
    val isbn10:String?,
    val isbn13: String?,
    val summary: String?,
    val publisher: String?,
    val pageCount: Int?,
    val publishedDate: String?,
    val image: String?,
    val modificationDate: Instant?,
    val authors: List<AuthorDto>?,
    val tags: List<TagDto>?,
    val series: String?,
    val numberInSeries: Double?,
    val googleId: String?,
    val amazonId: String?,
    val goodreadsId: String?,
    val librarythingId: String?,
    val language: String?,
)
data class BookWithUserBookDto(
    val id: UUID?,
    val creationDate: Instant?,
    val title: String,
    val isbn10:String?,
    val isbn13: String?,
    val summary: String?,
    val publisher: String?,
    val pageCount: Int?,
    val publishedDate: String?,
    val image: String?,
    val modificationDate: Instant?,
    val authors: List<AuthorDto>?,
    val tags: List<TagDto>?,
    val series: String?,
    val numberInSeries: Double?,
    val googleId: String?,
    val amazonId: String?,
    val goodreadsId: String?,
    val librarythingId: String?,
    val language: String?,
    val userBooks: List<UserBookWithoutBookDto>?
)
data class BookCreateDto(
    val id: UUID?,
//    val creationDate: Instant?,
    val title: String,
    val isbn10:String?,
    val isbn13: String?,
    val summary: String?,
    val image: String?,
    val publisher: String?,
    val pageCount: Int?,
    val publishedDate: String?,
    val authors: List<AuthorDto>?,
    val tags: List<TagDto>?,
    val series: String?,
    val numberInSeries: Double?,
    val googleId: String?,
    val amazonId: String?,
    val goodreadsId: String?,
    val librarythingId: String?,
    val language: String?,
)
data class BookUpdateDto(
    val title: String?,
    val isbn10:String?,
    val isbn13: String?,
    val summary: String?,
    val image: String?,
    val publisher: String?,
    val pageCount: Int?,
    val publishedDate: String?,
    val authors: List<AuthorDto>?,
    val tags: List<TagDto>?,
    val series: String?,
    val numberInSeries: Double?,
    val googleId: String?,
    val amazonId: String?,
    val goodreadsId: String?,
    val librarythingId: String?,
    val language: String?,
)
data class AuthorDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val name: String,
    val biography: String?,
    val dateOfBirth: String?,
    val dateOfDeath: String?,
    val image: String?
)
data class AuthorUpdateDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val name: String?,
    val biography: String?,
    val dateOfBirth: String?,
    val dateOfDeath: String?,
    val image: String?
)
data class AuthorWithBooksDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val name: String,
    val biography: String?,
    val dateOfBirth: String?,
    val dateOfDeath: String?,
    val image: String?,
    val books: List<BookDto>?
)
data class TagDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val name: String,
)
data class TagWithBooksDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val name: String,
    val books: List<BookWithUserBookDto>?
)
fun fromBookCreateDto(dto: BookCreateDto): BookUpdateDto {
    return BookUpdateDto(
        title = dto.title,
    isbn10= dto.isbn10,
    isbn13=dto.isbn13,
    summary=dto.summary,
    image= dto.image,
    publisher=dto.publisher,
    pageCount=dto.pageCount,
    publishedDate=dto.publishedDate,
    authors=dto.authors,
    tags=dto.tags,
    series=dto.series,
    numberInSeries=dto.numberInSeries,
    googleId=dto.googleId,
    amazonId=dto.amazonId,
    goodreadsId=dto.goodreadsId,
    librarythingId=dto.librarythingId,
    language=dto.language,
    )
}