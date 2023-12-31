package io.github.bayang.jelu

import io.github.bayang.jelu.dao.ImportSource
import io.github.bayang.jelu.dao.ReadingEventType
import io.github.bayang.jelu.dto.AuthorDto
import io.github.bayang.jelu.dto.BookCreateDto
import io.github.bayang.jelu.dto.CreateUserBookDto
import io.github.bayang.jelu.dto.ImportConfigurationDto
import io.github.bayang.jelu.dto.TagDto
import java.time.Instant

fun createUserBookDto(bookDto: BookCreateDto, lastReadingEvent: ReadingEventType? = null, lastreadingEventDate: Instant? = null, toRead: Boolean = false, owned: Boolean? = true, borrowed: Boolean = false): CreateUserBookDto {
    return CreateUserBookDto(
        personalNotes = "test personal notes\nwith a newline",
        lastReadingEvent = lastReadingEvent,
        lastReadingEventDate = lastreadingEventDate,
        owned = owned,
        toRead = toRead,
        percentRead = null,
        book = bookDto,
        borrowed = borrowed,
        currentPageNumber = null,
    )
}

fun bookDto(title: String = "title1", withTags: Boolean = false): BookCreateDto {
    return BookCreateDto(
        id = null,
        title = title,
        isbn10 = "1566199093",
        isbn13 = "9781566199094 ",
        summary = "This is a test summary\nwith a newline",
        image = "",
        publisher = "test-publisher",
        pageCount = 50,
        publishedDate = "",
        // seriesBak = "",
        authors = mutableListOf(authorDto()),
        // numberInSeries = null,
        tags = if (withTags) tags() else emptyList(),
        goodreadsId = "4321abc",
        googleId = "1234",
        librarythingId = "",
        language = "",
        amazonId = "",
    )
}

fun authorDto(name: String = "test author"): AuthorDto {
    return AuthorDto(id = null, creationDate = null, modificationDate = null, name = name, image = "", dateOfBirth = "", dateOfDeath = "", biography = "author bio", facebookPage = null, goodreadsPage = null, instagramPage = null, notes = null, officialPage = null, twitterPage = null, wikipediaPage = "https://wikipedia.org")
}

fun tags(): List<TagDto> {
    val tags = mutableListOf<TagDto>()
    tags.add(tagDto())
    tags.add(tagDto("fantasy"))
    return tags
}

fun tagDto(name: String = "science fiction"): TagDto {
    return TagDto(id = null, creationDate = null, modificationDate = null, name)
}

fun importConfigurationDto(): ImportConfigurationDto {
    return ImportConfigurationDto(shouldFetchMetadata = false, shouldFetchCovers = false, ImportSource.GOODREADS)
}
