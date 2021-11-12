package io.github.bayang.jelu.dto

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
    val modificationDate: Instant?,
    val authors: List<AuthorDto>?
)
data class BookUpdateDto(
    val id: UUID?,
    val creationDate: Instant?,
    val title: String?,
    val isbn10:String?,
    val isbn13: String?,
    val summary: String?,
    val publisher: String?,
    val pageCount: Int?,
    val publishedDate: String?,
    val modificationDate: Instant?,
    val authors: List<AuthorDto>?
)
data class AuthorDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val name: String,
)
data class AuthorUpdateDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val name: String?,
)
data class AuthorWithBooksDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val name: String,
    val books: List<BookDto>?
)