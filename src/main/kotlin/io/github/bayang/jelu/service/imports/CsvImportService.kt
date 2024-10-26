package io.github.bayang.jelu.service.imports

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dao.ImportEntity
import io.github.bayang.jelu.dao.ImportSource
import io.github.bayang.jelu.dao.MessageCategory
import io.github.bayang.jelu.dao.ProcessingStatus
import io.github.bayang.jelu.dao.ReadingEventType
import io.github.bayang.jelu.dao.Visibility
import io.github.bayang.jelu.dto.AuthorDto
import io.github.bayang.jelu.dto.BookCreateDto
import io.github.bayang.jelu.dto.BookDto
import io.github.bayang.jelu.dto.CreateReadingEventDto
import io.github.bayang.jelu.dto.CreateReviewDto
import io.github.bayang.jelu.dto.CreateUserBookDto
import io.github.bayang.jelu.dto.CreateUserMessageDto
import io.github.bayang.jelu.dto.ImportConfigurationDto
import io.github.bayang.jelu.dto.ImportDto
import io.github.bayang.jelu.dto.LibraryFilter
import io.github.bayang.jelu.dto.MetadataDto
import io.github.bayang.jelu.dto.MetadataRequestDto
import io.github.bayang.jelu.dto.SeriesOrderDto
import io.github.bayang.jelu.dto.TagDto
import io.github.bayang.jelu.dto.UserBookLightDto
import io.github.bayang.jelu.dto.UserBookUpdateDto
import io.github.bayang.jelu.service.BookService
import io.github.bayang.jelu.service.ImportService
import io.github.bayang.jelu.service.ReadingEventService
import io.github.bayang.jelu.service.ReviewService
import io.github.bayang.jelu.service.UserMessageService
import io.github.bayang.jelu.service.UserService
import io.github.bayang.jelu.service.metadata.FetchMetadataService
import io.github.bayang.jelu.service.metadata.providers.CalibreMetadataProvider
import io.github.bayang.jelu.utils.toInstant
import mu.KotlinLogging
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.apache.commons.validator.routines.ISBNValidator
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import java.util.stream.Collectors
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

const val ISBN_PREFIX = "=\""

const val TO_READ = "to-read"

const val READ = "read"

const val CURRENTLY_READING = "currently-reading"

/**
 * From storygraph
 */
const val DROPPED = "did-not-finish"

/**
 * Works for goodreads and storygraph
 */
val goodreadsDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

@Service
class CsvImportService(
    private val properties: JeluProperties,
    private val importService: ImportService,
    private val fetchMetadataService: FetchMetadataService,
    private val bookService: BookService,
    private val userService: UserService,
    private val readingEventService: ReadingEventService,
    private val userMessageService: UserMessageService,
    private val reviewService: ReviewService,
) {

    // maybe later : use coroutines ?
    @Async
    fun import(file: File, user: UUID, importConfig: ImportConfigurationDto) {
        val start = System.currentTimeMillis()
        val userEntity = userService.findUserEntityById(user).toUserDto()
        val nowString: String = OffsetDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
        try {
            userMessageService.save(
                CreateUserMessageDto(
                    "Import started at $nowString",
                    null,
                    MessageCategory.INFO,
                ),
                userEntity,
            )
        } catch (e: Exception) {
            logger.error(e) { "failed to save message for ${file.absolutePath} import" }
        }
        // put file content in db
        parse(file, user, importConfig)
        var end = System.currentTimeMillis()
        var deltaInSec = (end - start) / 1000
        logger.info { "csv parsing of ${file.absolutePath} ended after : $deltaInSec seconds" }
        // reset status of previous tasks, in case an import was started but not finished
        importService.updateStatus(ProcessingStatus.PROCESSING, ProcessingStatus.SAVED, user)
        // process data from db
        val (success, failures) = importFromDb(user, importConfig)
        val renamed = file.renameTo(File(file.parent, "${file.name}.imported"))
        logger.debug { "File ${file.absolutePath} was successfully renamed after processing : $renamed" }
        end = System.currentTimeMillis()
        deltaInSec = (end - start) / 1000
        logger.info { "Import for ${file.absolutePath} ended after : $deltaInSec seconds, with $success imports and $failures failures" }
        try {
            userMessageService.save(
                CreateUserMessageDto(
                    "Import for ${file.absolutePath} ended after : $deltaInSec seconds, with $success imports and $failures failures",
                    null,
                    MessageCategory.SUCCESS,
                ),
                userEntity,
            )
        } catch (e: Exception) {
            logger.error(e) { "failed to save message for ${file.absolutePath} import" }
        }
    }

    fun importFromDb(user: UUID, importConfig: ImportConfigurationDto): Pair<Long, Long> {
        var importEntities: List<ImportEntity>? = mutableListOf()
        var success: Long = 0
        var failures: Long = 0
        while (importEntities != null) {
            importEntities = importService.getByprocessingStatusAndUser(ProcessingStatus.SAVED, user)
            if (importEntities.isEmpty()) {
                importEntities = null
                break
            }
            for (entity in importEntities) {
                when (importEntity(entity, user, importConfig)) {
                    ProcessingStatus.IMPORTED -> success++
                    ProcessingStatus.ERROR -> failures++
                    else -> continue
                }
            }
        }
        return Pair(success, failures)
    }

    @Transactional
    private fun importEntity(importEntity: ImportEntity, user: UUID, importConfig: ImportConfigurationDto): ProcessingStatus {
        Thread.sleep(Random.nextLong(from = 200, until = 800))
        try {
            importService.updateStatus(importEntity.id.value, ProcessingStatus.PROCESSING)
            var metadata = MetadataDto()
            if (importEntity.shouldFetchMetadata && !properties.metadata.calibre.path.isNullOrBlank()) {
                val isbn: String = getIsbn(importEntity)
                if (isbn.isNotBlank()) {
                    var config = mutableMapOf<String, String>()
                    config[CalibreMetadataProvider.onlyUseCorePlugins] = "true"
                    config[CalibreMetadataProvider.fetchCover] = importConfig.shouldFetchCovers.toString()
                    metadata = fetchMetadataService
                        .fetchMetadata(MetadataRequestDto(isbn), config)
                        .block()!!
                } else {
                    logger.debug { "no isbn on entity ${importEntity.id}, not fetching metadata" }
                }
            }
            val userEntity = userService.findUserEntityById(user).toUserDto()
            val book: BookCreateDto = fillBook(importEntity, metadata)
            if (book.title.isBlank() && (book.authors == null || book.authors!!.isEmpty())) {
                logger.error { "no title nor authors on entity ${importEntity.id} ${importEntity.isbn10} ${importEntity.isbn13} , not saving" }
                importService.updateStatus(importEntity.id.value, ProcessingStatus.IMPORTED)
                if (importConfig.importSource == ImportSource.ISBN_LIST) {
                    try {
                        val isbn10 = if (importEntity.isbn10.isNullOrBlank()) "" else importEntity.isbn10
                        val isbn13 = if (importEntity.isbn13.isNullOrBlank()) "" else importEntity.isbn13
                        userMessageService.save(
                            CreateUserMessageDto(
                                "no title nor authors for input $isbn10 $isbn13, not saving",
                                null,
                                MessageCategory.WARNING,
                            ),
                            userEntity,
                        )
                    } catch (e: Exception) {
                        logger.error(e) { "failed to save message for failed isbn fetch" }
                    }
                }
                return ProcessingStatus.ERROR
            }
            val tags = mutableListOf<TagDto>()
            val tagNames = mutableSetOf<String>()
            for (t in metadata.tags) {
                tagNames.add(t)
            }
            var shelves: List<String>? = null
            if (!importEntity.tags.isNullOrBlank()) {
                shelves = importEntity.tags?.split(",")
            }
            var readStatusFromShelves = ""
            if (shelves != null) {
                for (t in shelves) {
                    if (!isreadStatusShelf(t)) {
                        tagNames.add(t)
                    } else {
                        // read status shelves are mutually exclusives
                        readStatusFromShelves = t
                    }
                }
            }
            for (t in tagNames) {
                tags.add(TagDto(null, null, null, t))
            }
            val readStatusEnum: ReadingEventType? = readingStatus(readStatusFromShelves)
            book.tags = tags
            val booksPage: Page<BookDto> = bookService.findAll(null, importEntity.isbn10, importEntity.isbn13, null, null, null, null, Pageable.ofSize(20), userEntity, LibraryFilter.ANY)
            // first case : the book we try to import from csv already exists in DB,
            // try to see if user already has it attached to his account (and only update userbook), or create new userbook if not
            val savedUserBook: UserBookLightDto = if (!booksPage.isEmpty) {
                val bookFromDb: BookDto = booksPage.content[0]
                // user already have an userbook for this book
                if (bookFromDb.userBookId != null) {
                    // update userbook and book
                    // val userbook: UserBookWithoutBookDto = bookWithUserbook.userBooks[0]
                    val userbook: UserBookLightDto = bookService.findUserBookById(bookFromDb.userBookId)
                    // only update fields if they are not filled yet, do not overwrite previously manually filled data
                    bookService.update(
                        userbook.id!!,
                        UserBookUpdateDto(
                            if ((readStatusEnum == ReadingEventType.CURRENTLY_READING || readStatusEnum == ReadingEventType.DROPPED) && userbook.lastReadingEvent == null) readStatusEnum else null,
                            if (userbook.personalNotes.isNullOrBlank()) importEntity.personalNotes else null,
                            if (userbook.owned == null) importEntity.owned else null,
                            merge(book, bookFromDb),
                            if (readStatusFromShelves.equals(TO_READ, true) && userbook.toRead == null) true else null,
                            null,
                            null,
                            null,
                        ),
                        null,
                    )
                } else {
                    // update book only and create userbook
                    val userbook = CreateUserBookDto(
                        if (readStatusEnum == ReadingEventType.CURRENTLY_READING || readStatusEnum == ReadingEventType.DROPPED) readStatusEnum else null,
                        null,
                        importEntity.personalNotes,
                        importEntity.owned,
                        merge(book, bookFromDb),
                        if (readStatusFromShelves.equals(TO_READ, true)) true else null,
                        null,
                        null,
                        null,
                    )
                    bookService.save(userbook, userEntity, null)
                }
            } else {
                val userbook = CreateUserBookDto(
                    if (readStatusEnum == ReadingEventType.CURRENTLY_READING || readStatusEnum == ReadingEventType.DROPPED) readStatusEnum else null,
                    null,
                    importEntity.personalNotes,
                    importEntity.owned,
                    book,
                    if (readStatusFromShelves.equals(TO_READ, true)) true else null,
                    null,
                    null,
                    null,
                )
                bookService.save(userbook, userEntity, null)
            }
            // csv exports are inconsistents, sometimes readDates are filled but not the
            // associated bookshelves or the read number field...
            // so take everything into account and try to avoid duplicates
            var readsSaved = 0
            if (!importEntity.readDates.isNullOrBlank()) {
                val dates = importEntity.readDates!!.split(";")
                for (date in dates) {
                    try {
                        val parsedDate = LocalDate.parse(date, goodreadsDateFormatter)
                        // in case of multiple import of the same file
                        // do not create the same finished event twice if possible
                        if (!alreadyHasFinishedEventAtSameDate(savedUserBook, parsedDate)) {
                            readingEventService.save(CreateReadingEventDto(ReadingEventType.FINISHED, savedUserBook.book.id, toInstant(parsedDate), null), userEntity)
                        }
                        readsSaved++
                    } catch (e: Exception) {
                        logger.error { "failed to parse date from export $date" }
                    }
                }
            }
            var remainingToSave = 0
            if (importEntity.readCount != null && importEntity.readCount!! > readsSaved) {
                remainingToSave = importEntity.readCount!! - readsSaved
            }
            if (readStatusEnum != null &&
                readStatusEnum == ReadingEventType.FINISHED &&
                remainingToSave == 0 &&
                readsSaved == 0
            ) {
                remainingToSave++
            }
            // bookService.findUserBookById(savedUserBook.id)
            val nbAlreadyFinishedEvents = if (savedUserBook.readingEvents != null) savedUserBook.readingEvents.stream().filter { it.eventType == ReadingEventType.FINISHED }.count().toInt() else 0
            remainingToSave -= nbAlreadyFinishedEvents
            // we know the book has been read 3 times for example but don't have dates
            // just create reading events in the past at an arbitrary date
            // user will correct each event if he wants to
            val pastDate: LocalDate = LocalDate.of(1970, 1, 1)
            for (idx in 0 until remainingToSave) {
                readingEventService.save(
                    CreateReadingEventDto(
                        ReadingEventType.FINISHED,
                        savedUserBook.book.id,
                        toInstant(
                            pastDate.plusDays(
                                idx.toLong(),
                            ),
                        ),
                        null,
                    ),
                    userEntity,
                )
            }
            val rating: Int = if (importEntity.rating != null) (importEntity.rating!! * 2) else 0
            if (!importEntity.review.isNullOrEmpty() || rating > 0) {
                val existing = reviewService.find(
                    userEntity.id,
                    savedUserBook.book.id!!,
                    null,
                    null,
                    null,
                    Pageable.ofSize(10),
                )
                if (existing.isEmpty) {
                    reviewService.save(
                        CreateReviewDto(
                            Instant.now(),
                            if (importEntity.review.isNullOrEmpty()) "" else importEntity.review!!,
                            rating.toDouble(),
                            Visibility.PUBLIC,
                            savedUserBook.book.id,
                        ),
                        userEntity,
                    )
                }
            }
            importService.updateStatus(importEntity.id.value, ProcessingStatus.IMPORTED)
            return ProcessingStatus.IMPORTED
        } catch (e: Exception) {
            logger.error(e) { "failed to import entity ${importEntity.title}" }
            importService.updateStatus(importEntity.id.value, ProcessingStatus.ERROR)
            return ProcessingStatus.ERROR
        }
    }

    fun alreadyHasFinishedEventAtSameDate(userBook: UserBookLightDto, date: LocalDate): Boolean {
        if (userBook.readingEvents == null) {
            return false
        }
        return userBook.readingEvents.stream()
            .filter { it.eventType == ReadingEventType.FINISHED }
            .filter { it.creationDate != null }
            .map { LocalDate.ofInstant(it.creationDate, ZoneId.systemDefault()) }
            .filter { it.year == date.year && it.monthValue == date.monthValue && it.dayOfMonth == date.dayOfMonth }
            .count().toInt() > 0
    }

    private fun readingStatus(readStatusFromShelves: String): ReadingEventType? {
        return if (readStatusFromShelves.equals(CURRENTLY_READING, true)) {
            ReadingEventType.CURRENTLY_READING
        } else if (readStatusFromShelves.equals(DROPPED, true)) {
            ReadingEventType.DROPPED
        } else if (readStatusFromShelves.equals(READ, true)) {
            ReadingEventType.FINISHED
        } else {
            null
        }
    }

    /**
     * only update fields if they are not filled yet, do not overwrite previously manually filled data
     */
    private fun merge(incoming: BookCreateDto, dbBook: BookDto): BookCreateDto {
        val authors = mutableListOf<AuthorDto>()
        // only save authors not already in db
        if (incoming.authors != null) {
            val existingAuthorsNames: Set<String> = if (dbBook.authors != null) {
                dbBook.authors.stream().map { it.name }.map { it.lowercase() }.collect(Collectors.toSet())
            } else {
                setOf()
            }
            incoming.authors!!.filter { authorDto -> !existingAuthorsNames.contains(authorDto.name.lowercase()) }.forEach { authorDto -> authors.add(authorDto) }
        }

        val tags = mutableListOf<TagDto>()
        // only save tags not already in db
        if (incoming.tags != null) {
            val existingTagsNames: Set<String> = if (dbBook.tags != null) {
                dbBook.tags.stream().map { it.name }.map { it.lowercase() }.collect(Collectors.toSet())
            } else {
                setOf()
            }
            incoming.tags!!.filter { tagDto -> !existingTagsNames.contains(tagDto.name.lowercase()) }.forEach { tagDto -> tags.add(tagDto) }
        }
        val series = mutableListOf<SeriesOrderDto>()
        // only save series not already in db
        if (incoming.series != null) {
            val existingSeriesNames: Set<String> = if (dbBook.series != null) {
                dbBook.series.stream().map { it.name }.map { it.lowercase() }.collect(Collectors.toSet())
            } else {
                setOf()
            }
            incoming.series!!.filter { seriesDto -> !existingSeriesNames.contains(seriesDto.name.lowercase()) }.forEach { seriesDto -> series.add(seriesDto) }
        }
        return incoming.copy(
            id = dbBook.id,
            title = dbBook.title.ifBlank { incoming.title },
            isbn10 = if (dbBook.isbn10.isNullOrBlank()) incoming.isbn10 else null,
            isbn13 = if (dbBook.isbn13.isNullOrBlank()) incoming.isbn13 else null,
            summary = if (dbBook.summary.isNullOrBlank()) incoming.summary else null,
            publisher = if (dbBook.publisher.isNullOrBlank()) incoming.publisher else null,
            pageCount = if (dbBook.pageCount == null) incoming.pageCount else null,
            publishedDate = if (dbBook.publishedDate.isNullOrBlank()) incoming.publishedDate else null,
            series = series,
            // numberInSeries = if (dbBook.numberInSeries == null) incoming.numberInSeries else null,
            googleId = if (dbBook.googleId.isNullOrBlank()) incoming.googleId else null,
            amazonId = if (dbBook.amazonId.isNullOrBlank()) incoming.amazonId else null,
            goodreadsId = if (dbBook.goodreadsId.isNullOrBlank()) incoming.goodreadsId else null,
            librarythingId = if (dbBook.librarythingId.isNullOrBlank()) incoming.librarythingId else null,
            language = if (dbBook.language.isNullOrBlank()) incoming.language else null,
            // special case :
            // if image is null, update in bookservice will erase the existing image,
            // we must send the currently existing image
            image = if (dbBook.image.isNullOrBlank()) incoming.image else dbBook.image,
            authors = authors,
            tags = tags,
        )
    }

    fun fillBook(importEntity: ImportEntity, metadata: MetadataDto): BookCreateDto {
        val book = BookCreateDto()
        book.title = testValues(importEntity.title, metadata.title)
        book.language = metadata.language
        book.amazonId = metadata.amazonId
        book.goodreadsId = importEntity.goodreadsId
        book.googleId = metadata.googleId
        book.librarythingId = importEntity.librarythingId
        book.isbn10 = testValues(importEntity.isbn10, metadata.isbn10)
        book.isbn13 = testValues(importEntity.isbn13, metadata.isbn13)
        // book.numberInSeries = metadata.numberInSeries
        book.pageCount = importEntity.numberOfPages ?: metadata.pageCount
        book.publishedDate = testValues(importEntity.publishedDate, metadata.publishedDate)
        book.publisher = testValues(importEntity.publisher, metadata.publisher)
        book.summary = metadata.summary
        // book.seriesBak = metadata.series
        book.image = metadata.image
        val authorsStrings = mutableSetOf<String>()
        authorsStrings.addAll(metadata.authors)
        if (!importEntity.authors.isNullOrBlank()) {
            val split = importEntity.authors!!.split(",")
            authorsStrings.addAll(split)
        }
        val authors = mutableListOf<AuthorDto>()
        for (authorString in authorsStrings) {
            authors.add(AuthorDto(null, null, null, authorString, null, null, null, null, null, null, null, null, null, null, null))
        }
        val seriesString = metadata.series
        if (!seriesString.isNullOrBlank()) {
            book.series = listOf(SeriesOrderDto(name = seriesString, numberInSeries = metadata.numberInSeries))
        }
        book.authors = authors
        return book
    }

    fun isreadStatusShelf(bookshelf: String): Boolean {
        if (bookshelf.isNotBlank() &&
            (
                bookshelf.equals(TO_READ, true) ||
                    bookshelf.equals(CURRENTLY_READING, true) ||
                    bookshelf.equals(READ, true) ||
                    bookshelf.equals(DROPPED, true)
                )
        ) {
            return true
        }
        return false
    }

    fun testValues(first: String?, second: String?): String {
        return if (!first.isNullOrBlank()) {
            first
        } else if (!second.isNullOrBlank()) {
            second
        } else {
            ""
        }
    }

    private fun getIsbn(importEntity: ImportEntity): String {
        if (!importEntity.isbn13.isNullOrBlank()) {
            return importEntity.isbn13!!
        } else if (!importEntity.isbn10.isNullOrBlank()) {
            return importEntity.isbn10!!
        }
        return ""
    }

    /*
     * Goodreads
     *
     * 0 Book Id
     * 1 Title
     * 2 Author
     * 3 Author l-f
     * 4 Additional Authors
     * 5 ISBN
     * 6 ISBN13
     * 7 My Rating
     * 8 Average Rating
     * 9 Publisher
     * 10 Binding
     * 11 Number of Pages
     * 12 Year Published
     * 13 Original Publication Year
     * 14 Date Read
     * 15 Date Added
     * 16 Bookshelves
     * 17 Bookshelves with positions
     * 18 Exclusive Shelf
     * 19 My Review
     * 20 Spoiler
     * 21 Private Notes
     * 22 Read Count
     * 23 Recommended For
     * 24 Recommended By
     * 25 Owned Copies
     * 26 Original Purchase Date
     * 27 Original Purchase Location
     * 28 Condition
     * 29 Condition Description
     * 30 BCID
     *
     */

    fun parse(file: File, user: UUID, importConfig: ImportConfigurationDto) {
        if (importConfig.importSource == ImportSource.ISBN_LIST) {
            val validator: ISBNValidator = ISBNValidator.getInstance(false)
            val counter = AtomicLong()
            file.useLines { lines ->
                lines.forEach {
                    val dto = ImportDto()
                    dto.importSource = ImportSource.ISBN_LIST
                    if (validator.isValidISBN13(it)) {
                        dto.isbn13 = it
                    } else if (validator.isValidISBN10(it)) {
                        dto.isbn10 = it
                    }
                    if (!dto.isbn13.isNullOrBlank() || !dto.isbn10.isNullOrBlank()) {
                        importService.save(dto, ProcessingStatus.SAVED, user, importConfig.shouldFetchMetadata)
                        counter.incrementAndGet()
                    } else {
                        logger.info { "input line $it is not a valid ISBN, line is ignored" }
                    }
                }
            }
            logger.debug { "parsing finished, ${counter.get()} entries recorded" }
        } else {
            parseCsv(file, user, importConfig)
        }
    }

    fun parseCsv(file: File, user: UUID, importConfig: ImportConfigurationDto) {
        val parser = CSVFormat.DEFAULT
        val reader = file.bufferedReader(Charsets.UTF_8)

        // skip header line ourselves
        // CSVFormat.withSkipHeaderRecord() doesn't work
        reader.readLine()

        val parsed: CSVParser = parser.parse(reader)
        val iterator: Iterator<CSVRecord> = parsed.iterator()
        var count: Long = 0
        var dto: ImportDto? = null
        while (iterator.hasNext()) {
            try {
                val record: CSVRecord = iterator.next()
                dto = parseLine(record, importConfig.importSource)
                if (dto != null) {
                    // save in DB with user info
                    importService.save(dto, ProcessingStatus.SAVED, user, importConfig.shouldFetchMetadata)
                    count++
                }
            } catch (e: Exception) {
                logger.error { "Failed to process line or save data from file ${file.absolutePath}, line : ${dto?.title}" }
            }
        }
        logger.debug { "parsing finished, $count entries recorded" }
    }

    private fun parseLine(record: CSVRecord, importSource: ImportSource): ImportDto? {
        return when (importSource) {
            ImportSource.GOODREADS -> parseGoodreadsLine(record)
            ImportSource.STORYGRAPH -> parseStorygraphLine(record)
            ImportSource.LIBRARYTHING -> parseLibrarythingLine(record)
            ImportSource.ISBN_LIST -> null
        }
    }

    // TODO
    private fun parseLibrarythingLine(record: CSVRecord): ImportDto? {
        val dto = ImportDto()
        dto.importSource = ImportSource.LIBRARYTHING
        return dto
    }

    // TODO
    private fun parseStorygraphLine(record: CSVRecord): ImportDto? {
        val dto = ImportDto()
        dto.importSource = ImportSource.STORYGRAPH
        return dto
    }

    private fun parseGoodreadsLine(record: CSVRecord): ImportDto? {
        val dto = ImportDto()
        val rawIsbn10 = record.get(5)
        val parsedIsbn10 = parseIsbn(rawIsbn10)
        val rawIsbn13 = record.get(6)
        val parsedIsbn13 = parseIsbn(rawIsbn13)
        val author = cleanString(record.get(2))
        val title = cleanString(record.get(1))
        val goodreadsId = cleanString(record.get(0))
        // we want at least an isbn
        if (parsedIsbn10.isBlank() && parsedIsbn13.isBlank()) {
            logger.debug { "Missing isbn for line with goodreadsId $goodreadsId, title $title and author $author, import it yourself manually" }
            return null
        } else {
            val additionalAuthors = cleanString(record.get(4))
            val authors = mutableSetOf<String>()
            authors.add(author)
            if (additionalAuthors.isNotBlank()) {
                val split = additionalAuthors.split(",")
                authors.addAll(split)
            }
            dto.authors = authors.joinToString(separator = ",")
            dto.title = title
            dto.goodreadsId = goodreadsId
            if (parsedIsbn10.isNotBlank()) {
                dto.isbn10 = parsedIsbn10
            }
            if (parsedIsbn13.isNotBlank()) {
                dto.isbn13 = parsedIsbn13
            }
            val bookshelves = cleanString(record.get(16))
            if (bookshelves.isNotBlank()) {
                bookshelves.split(",").forEach { dto.tags.add(it.trim()) }
            }
            val exclusiveShelf = cleanString(record.get(18))
            if (exclusiveShelf.isNotBlank()) {
                dto.tags.add(exclusiveShelf.trim())
            }
            dto.numberOfPages = parseNumber(record.get(11))
            dto.personalNotes = cleanString(record.get(21))
            dto.publishedDate = cleanString(record.get(12))
            dto.publisher = cleanString(record.get(9))
            dto.readCount = parseNumber(record.get(22))
            dto.readDates = cleanString(record.get(14))
            // goodreads csv export changed columns in 2022 apparently
            // new exports have only 24 columns, older ones have 31
            val ownedCopies = if (record.size() > 24 && record.isSet(25)) {
                parseNumber(record.get(25))
            } else {
                parseNumber(record.get(23))
            }
            if (ownedCopies != null && ownedCopies > 0) {
                dto.owned = true
            }
            dto.review = cleanString(record.get(19))
            dto.rating = parseNumber(record.get(7))
            dto.importSource = ImportSource.GOODREADS
            return dto
        }
    }

    fun cleanString(input: String?): String {
        if (input.isNullOrBlank()) {
            return ""
        }
        return input.trim()
    }

    /**
     * eg in raw goodreads csv : "=""9782841720538""" ...
     */
    private fun parseIsbn(input: String): String {
        if (input.isBlank()) {
            return ""
        } else {
            var isbn = ""
            if (input.startsWith(ISBN_PREFIX)) {
                isbn = input.substring(ISBN_PREFIX.length)
            }
            if (isbn.length < 10) {
                return ""
            }
            return isbn.substring(0, isbn.length - 1)
        }
    }

    private fun parseNumber(input: String): Int? {
        return if (input.isBlank()) {
            null
        } else {
            try {
                Integer.parseInt(input, 10)
            } catch (e: Exception) {
                logger.error { "failed to parse number $input" }
                null
            }
        }
    }
}
