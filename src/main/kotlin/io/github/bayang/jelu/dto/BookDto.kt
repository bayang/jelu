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
    val series: String?,
    val numberInSeries: Double?
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
    val series: String?,
    val numberInSeries: Double?,
)
//data class BookUpdateDto(
//    val title: String?,
//    val isbn10:String?,
//    val isbn13: String?,
//    val summary: String?,
//    val image: String?,
//    val publisher: String?,
//    val pageCount: Int?,
//    val publishedDate: String?,
//    val authors: List<AuthorDto>?,
//    val series: String?,
//    val numberInSeries: Double?
//)
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