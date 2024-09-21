package io.github.bayang.jelu.dto

import java.time.Instant
import java.util.UUID

data class BookDto(
    val id: UUID?,
    val creationDate: Instant?,
    val title: String,
    val isbn10: String?,
    val isbn13: String?,
    val summary: String?,
    val publisher: String?,
    val pageCount: Int?,
    val publishedDate: String?,
    val image: String?,
    val modificationDate: Instant?,
    val authors: List<AuthorDto>?,
    val translators: List<AuthorDto>?,
    val tags: List<TagDto>?,
    val series: List<SeriesOrderDto>?,
    val googleId: String?,
    val amazonId: String?,
    val goodreadsId: String?,
    val librarythingId: String?,
    val language: String?,
    val userBookId: UUID?,
    val userbook: UserBookLightWithoutBookDto?,
)

data class BookCreateDto(
    val id: UUID? = null,
    var title: String = "",
    var isbn10: String? = null,
    var isbn13: String? = null,
    var summary: String? = null,
    var image: String? = null,
    var publisher: String? = null,
    var pageCount: Int? = null,
    var publishedDate: String? = null,
    var authors: List<AuthorDto>? = null,
    var translators: List<AuthorDto>? = null,
    var tags: List<TagDto>? = null,
    var series: List<SeriesOrderDto>? = null,
    var googleId: String? = null,
    var amazonId: String? = null,
    var goodreadsId: String? = null,
    var librarythingId: String? = null,
    var language: String? = null,
)

data class BookUpdateDto(
    val title: String?,
    val isbn10: String?,
    val isbn13: String?,
    val summary: String?,
    val image: String?,
    val publisher: String?,
    val pageCount: Int?,
    val publishedDate: String?,
    var authors: List<AuthorDto>?,
    var translators: List<AuthorDto>?,
    val tags: List<TagDto>?,
    val googleId: String?,
    val amazonId: String?,
    val goodreadsId: String?,
    val librarythingId: String?,
    val language: String?,
    var series: List<SeriesOrderDto>?,
)

data class AuthorDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val name: String,
    val biography: String?,
    val dateOfBirth: String?,
    val dateOfDeath: String?,
    val image: String?,
    val notes: String?,
    val officialPage: String?,
    val wikipediaPage: String?,
    val goodreadsPage: String?,
    val twitterPage: String?,
    val facebookPage: String?,
    val instagramPage: String?,
)

data class AuthorUpdateDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val name: String?,
    val biography: String?,
    val dateOfBirth: String?,
    val dateOfDeath: String?,
    val image: String?,
    val notes: String?,
    val officialPage: String?,
    val wikipediaPage: String?,
    val goodreadsPage: String?,
    val twitterPage: String?,
    val facebookPage: String?,
    val instagramPage: String?,
)

data class TagDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val name: String,
)

fun fromBookCreateDto(dto: BookCreateDto): BookUpdateDto {
    return BookUpdateDto(
        title = dto.title,
        isbn10 = dto.isbn10,
        isbn13 = dto.isbn13,
        summary = dto.summary,
        image = dto.image,
        publisher = dto.publisher,
        pageCount = dto.pageCount,
        publishedDate = dto.publishedDate,
        authors = dto.authors,
        translators = dto.translators,
        tags = dto.tags,
        googleId = dto.googleId,
        amazonId = dto.amazonId,
        goodreadsId = dto.goodreadsId,
        librarythingId = dto.librarythingId,
        language = dto.language,
        series = dto.series,
    )
}

data class SeriesDto(
    val id: UUID?,
    val creationDate: Instant?,
    val modificationDate: Instant?,
    val name: String,
    var avgRating: Double? = null,
    var userRating: Double? = null,
    var description: String?,
)
data class SeriesOrderDto(
    val seriesId: UUID? = null,
    var name: String,
    var numberInSeries: Double?,
)

data class SeriesCreateDto(
    val name: String,
    val rating: Double?,
    val description: String?,
)

data class SeriesUpdateDto(
    val name: String?,
    val rating: Double?,
    val description: String?,
)

data class CreateSeriesRatingDto(
    val seriesId: UUID,
    var rating: Double,
)
data class SeriesRatingDto(
    val seriesId: UUID,
    val userId: UUID,
    var rating: Double,
    val creationDate: Instant?,
    val modificationDate: Instant?,
)
