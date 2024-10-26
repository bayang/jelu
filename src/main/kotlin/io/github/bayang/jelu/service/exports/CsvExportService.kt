package io.github.bayang.jelu.service.exports

import io.github.bayang.jelu.config.EXPORTS_PREFIX
import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dao.MessageCategory
import io.github.bayang.jelu.dao.ReadingEventType
import io.github.bayang.jelu.dto.CreateUserMessageDto
import io.github.bayang.jelu.dto.UserBookWithoutEventsAndUserDto
import io.github.bayang.jelu.dto.UserDto
import io.github.bayang.jelu.service.BookService
import io.github.bayang.jelu.service.ReadingEventService
import io.github.bayang.jelu.service.UserMessageService
import io.github.bayang.jelu.service.imports.CURRENTLY_READING
import io.github.bayang.jelu.service.imports.TO_READ
import io.github.bayang.jelu.service.imports.goodreadsDateFormatter
import io.github.bayang.jelu.utils.lastEventDate
import mu.KotlinLogging
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.apache.commons.csv.QuoteMode
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID
import java.util.stream.Collectors

private val logger = KotlinLogging.logger {}

@Service
class CsvExportService(
    private val bookService: BookService,
    private val properties: JeluProperties,
    private val readingEventService: ReadingEventService,
    private val userMessageService: UserMessageService,
) {

    /**
     * Columns from Title to Bookshelves are used for Goodreads reimport.
     * see https://help.goodreads.com/s/article/How-to-import-my-books-from-other-cataloging-services-1553870934585
     * Other columns are generic data that could be used elsewhere.
     */
    fun export(user: UserDto, locale: Locale) {
        val format: CSVFormat = CSVFormat.Builder.create(CSVFormat.EXCEL).setQuoteMode(QuoteMode.MINIMAL).build()
        val start = System.currentTimeMillis()
        val userId = user.id
        logger.debug { "beginning csv export" }
        val nowString: String = OffsetDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
        val nowPretty: String = try {
            LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM).withLocale(locale))
        } catch (e: Exception) {
            nowString
        }
        val destFileName = "jelu-export-${user.login}-$nowString.csv"
        try {
            userMessageService.save(
                CreateUserMessageDto(
                    "Export started at $nowPretty",
                    null,
                    MessageCategory.INFO,
                ),
                user,
            )
        } catch (e: Exception) {
            logger.error(e) { "failed to save message for $destFileName export" }
        }
        var currentPage = 0
        val pageSize = 100
        var books: Page<UserBookWithoutEventsAndUserDto>
        var count: Long = 0
        val destFile = File(properties.files.imports, destFileName)
        logger.debug { "target export file at ${destFile.absolutePath}" }
        try {
            CSVPrinter(BufferedWriter(FileWriter(destFile)), format).use { printer ->
                printer.printRecord("Title", "Author", "ISBN", "Publisher", "Date Read", "Shelves", "Bookshelves", "read_dates", "tags", "authors", "isbn10", "isbn13", "owned", "dropped_dates", "currently_reading")
                do {
                    books = bookService.findUserBookByCriteria(userId!!, null, null, null, null, null, PageRequest.of(currentPage, pageSize))
                    currentPage++
                    logger.debug { "current $currentPage" }
                    count += books.content.size
                    processBooks(books, printer, userId)
                } while (!books.isEmpty)
            }
        } catch (ex: Exception) {
            logger.error(ex) { "Error processing block of entries for export at page $currentPage" }
        }
        val end = System.currentTimeMillis()
        val msg = "$count books have been processed in ${end - start} milliseconds"
        logger.debug { msg }
        try {
            userMessageService.save(
                CreateUserMessageDto(
                    "Export completed : $msg",
                    "$EXPORTS_PREFIX/$destFileName",
                    MessageCategory.SUCCESS,
                ),
                user,
            )
        } catch (e: Exception) {
            logger.error(e) { "failed to save message for $destFileName export" }
        }
    }

    private fun processBooks(books: Page<UserBookWithoutEventsAndUserDto>, printer: CSVPrinter, userId: UUID) {
        books.content.forEach {
            logger.debug { it.book.title }
            printer.printRecord(
                it.book.title,
                if (it.book.authors.isNullOrEmpty()) "" else it.book.authors[0].name,
                isbn(it),
                if (it.book.publisher.isNullOrBlank()) "" else it.book.publisher,
                dateRead(it),
                shelves(it),
                bookShelves(it),
                listOfDatesForEvent(it, userId, ReadingEventType.FINISHED),
                tags(it),
                authors(it),
                if (it.book.isbn10.isNullOrBlank()) "" else it.book.isbn10,
                if (it.book.isbn13.isNullOrBlank()) "" else it.book.isbn13,
                if (it.owned == true) "true" else "",
                listOfDatesForEvent(it, userId, ReadingEventType.DROPPED),
                listOfDatesForEvent(it, userId, ReadingEventType.CURRENTLY_READING),
            )
        }
    }

    fun isbn(userbook: UserBookWithoutEventsAndUserDto): String {
        return if (!userbook.book.isbn13.isNullOrBlank()) {
            userbook.book.isbn13
        } else if (!userbook.book.isbn10.isNullOrBlank()) {
            userbook.book.isbn10
        } else {
            ""
        }
    }

    fun dateRead(userbook: UserBookWithoutEventsAndUserDto): String {
        return if (userbook.lastReadingEventDate != null) {
            toDateString(userbook.lastReadingEventDate)
        } else {
            ""
        }
    }

    fun toDateString(instant: Instant?): String {
        return if (instant != null) {
            LocalDate.ofInstant(instant, ZoneId.systemDefault()).format(
                goodreadsDateFormatter,
            )
        } else {
            ""
        }
    }

    fun shelves(userbook: UserBookWithoutEventsAndUserDto): String {
        return if (userbook.lastReadingEvent == ReadingEventType.CURRENTLY_READING) {
            CURRENTLY_READING
        } else if (userbook.toRead == true) {
            TO_READ
        } else {
            ""
        }
    }

    fun bookShelves(userbook: UserBookWithoutEventsAndUserDto): String {
        return if (!userbook.book.tags.isNullOrEmpty()) {
            userbook.book.tags.stream().map { it.name.replace(" ", "", true) }.collect(Collectors.joining(" "))
        } else {
            ""
        }
    }

    fun listOfDatesForEvent(userbook: UserBookWithoutEventsAndUserDto, userId: UUID, eventType: ReadingEventType): String {
        val reads = readingEventService.findAll(listOf(eventType), userId, userbook.book.id, null, null, null, null, Pageable.ofSize(100))
        if (!reads.isEmpty) {
            return reads.content.stream().map { lastEventDate(it) }.sorted().map { toDateString(it) }.collect(Collectors.joining(","))
        }
        return ""
    }

    fun tags(userbook: UserBookWithoutEventsAndUserDto): String {
        return if (!userbook.book.tags.isNullOrEmpty()) {
            userbook.book.tags.stream().map { it.name }.collect(Collectors.joining(","))
        } else {
            ""
        }
    }

    fun authors(userbook: UserBookWithoutEventsAndUserDto): String {
        return if (!userbook.book.authors.isNullOrEmpty()) {
            userbook.book.authors.stream().map { it.name }.collect(Collectors.joining(","))
        } else {
            return ""
        }
    }
}
