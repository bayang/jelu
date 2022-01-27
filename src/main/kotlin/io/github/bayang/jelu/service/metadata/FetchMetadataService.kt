package io.github.bayang.jelu.service.metadata

import com.ctc.wstx.stax.WstxInputFactory
import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dto.MetadataDto
import io.github.bayang.jelu.errors.JeluException
import io.github.bayang.jelu.utils.sanitizeHtml
import io.github.bayang.jelu.utils.slugify
import mu.KotlinLogging
import org.apache.commons.validator.routines.ISBNValidator
import org.codehaus.staxmate.SMInputFactory
import org.codehaus.staxmate.`in`.SMHierarchicCursor
import org.codehaus.staxmate.`in`.SMInputCursor
import org.springframework.stereotype.Service
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File

private val logger = KotlinLogging.logger {}
const val FILE_PREFIX = "meta-import-"

@Service
class FetchMetadataService(
    private val properties: JeluProperties,
) {

    val factory: SMInputFactory = SMInputFactory(WstxInputFactory())
    val validator: ISBNValidator = ISBNValidator.getInstance(false)

    fun fetchMetadata(
        isbn: String?,
        title: String?,
        authors: String?,
        onlyUseCorePlugins: Boolean = false,
        fetchCover: Boolean = true
    ): MetadataDto {
        if (isbn.isNullOrBlank() && title.isNullOrBlank() && authors.isNullOrBlank()) {
            throw JeluException("At least one of isbn, authors or title is required to fetch metadata")
        }
        var bookFileName: String = FILE_PREFIX
        val commandArray: MutableList<String> = mutableListOf(properties.metadata.calibre.path!!, "-o", "-d 90")
        var fileNameComplete = false
        if (!isbn.isNullOrBlank()) {
            bookFileName += isbn
            fileNameComplete = true
            commandArray.add("-i")
            commandArray.add(isbn)
        }
        if (!title.isNullOrBlank()) {
            commandArray.add("-t")
            commandArray.add(title)
            if (! fileNameComplete) {
                bookFileName += slugify(title)
                fileNameComplete = true
            }
        }
        if (!authors.isNullOrBlank()) {
            commandArray.add("-a")
            commandArray.add(authors)
            if (!fileNameComplete) {
                bookFileName += slugify(authors)
                fileNameComplete = true
            }
        }
        if (onlyUseCorePlugins) {
            commandArray.add("-p")
            commandArray.add("Google")
            commandArray.add("-p")
            commandArray.add("Amazon.com")
        }
        bookFileName += ".jpg"
        val targetCover = File(properties.files.images, bookFileName)
        if (fetchCover) {
            commandArray.add("-c")
            commandArray.add(targetCover.absolutePath)
        }
        val builder = ProcessBuilder()

        builder.command(commandArray)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
        try {
            val process: Process = builder.start()
            val exitVal = process.waitFor()
            if (exitVal == 0) {
                val output: String = process.inputStream.bufferedReader().readText()
                logger.trace { "fetch metadata output $output" }
                val parseOpf: MetadataDto = parseOpf(output)
                if (!isbn.isNullOrBlank()) {
                    if (validator.isValidISBN13(isbn) && parseOpf.isbn13.isNullOrBlank()) {
                        parseOpf.isbn13 = isbn
                    } else if (validator.isValidISBN10(isbn) && parseOpf.isbn10.isNullOrBlank()) {
                        parseOpf.isbn10 = isbn
                    }
                }
                if (targetCover.exists() && targetCover.length() > 0) {
                    parseOpf.image = targetCover.name
                }
                return parseOpf
            } else {
                logger.error { "fetch ebookmetadata process exited abnormally with code $exitVal" }
            }
        } catch (e: Exception) {
            logger.error(e) { "failure while calling fetch-ebook-metadata process" }
        }
        return MetadataDto()
    }

    fun parseOpf(input: String): MetadataDto {
        val stream = BufferedInputStream(ByteArrayInputStream(input.toByteArray(Charsets.UTF_8)))
        val root: SMHierarchicCursor = factory.rootElementCursor(stream)
        val dto = MetadataDto()
        try {
            root.advance()
            val rootChildrenCursor = root.childElementCursor()
            while (rootChildrenCursor.next != null) {
                when (rootChildrenCursor.localName) {
                    METADATA -> metadata(rootChildrenCursor.childElementCursor(), dto)
                    GUIDE -> logPart(rootChildrenCursor.childElementCursor())
                    else -> {
                        logger.debug { "rootChildren name ${rootChildrenCursor.localName} qname ${rootChildrenCursor.qName}" }
                        logPart(rootChildrenCursor.childElementCursor())
                    }
                }
            }
            logger.debug { "parsed dto $dto" }
        } catch (e: Exception) {
            logger.error(e) { "failure while parsing opf metadata from calibre" }
        } finally {
            root.streamReader.closeCompletely()
        }
        return dto
    }

    private fun metadata(childElementCursor: SMInputCursor, dto: MetadataDto) {
        while (childElementCursor.next != null) {
            when (childElementCursor.localName) {
                IDENTIFIER -> {
                    when (childElementCursor.getAttrValue("scheme")) {
                        "GOODREADS" -> dto.goodreadsId = childElementCursor.elemStringValue
                        "GOOGLE" -> dto.googleId = childElementCursor.elemStringValue
                        "AMAZON" -> dto.amazonId = childElementCursor.elemStringValue
                        "ISBN" -> {
                            val isbn = childElementCursor.elemStringValue
                            if (validator.isValidISBN13(isbn)) {
                                dto.isbn13 = isbn
                            } else if (validator.isValidISBN10(isbn)) {
                                dto.isbn10 = isbn
                            }
                        }
                        else -> logger.debug { "unhandled identifier scheme ${childElementCursor.getAttrValue("scheme")}" }
                    }
                }
                TITLE -> dto.title = childElementCursor.elemStringValue
                CREATOR -> {
                    when (childElementCursor.getAttrValue("role")) {
                        // sometimes we receive mutliple authors on one line, separated by ;
                        "aut" -> dto.authors.addAll(splitValues(childElementCursor.elemStringValue))
                        else -> logger.debug { "unhandled creator role ${childElementCursor.getAttrValue("role")}" }
                    }
                }
                DATE -> dto.publishedDate = childElementCursor.elemStringValue
                DESCRIPTION -> dto.summary = sanitizeHtml(childElementCursor.elemStringValue)
                PUBLISHER -> dto.publisher = childElementCursor.elemStringValue
                LANGUAGE -> dto.language = childElementCursor.elemStringValue
                SUBJECT -> dto.tags.add(childElementCursor.elemStringValue)
                META -> {
                    when (childElementCursor.getAttrValue("name")) {
                        "calibre:series" -> dto.series = childElementCursor.getAttrValue("content")
                        "calibre:series_index" -> dto.numberInSeries = childElementCursor.getAttrValue("content").toDoubleOrNull()
                        "calibre:author_link_map" -> logger.debug { "author_link_map ${childElementCursor.getAttrValue("content")}" }
                        else -> logger.debug { "unhandled meta name ${childElementCursor.getAttrValue("name")}" }
                    }
                }
            }
        }
    }

    private fun splitValues(elemStringValue: String?): Collection<String> {
        return if (elemStringValue.isNullOrBlank()) {
            emptySet()
        } else {
            if (elemStringValue.contains(";")) {
                elemStringValue.split(";").filter { !it.isNullOrBlank() }.map { it.trim() }.toSet()
            } else {
                setOf(elemStringValue.trim())
            }
        }
    }

    private fun logPart(childElementCursor: SMInputCursor) {
        while (childElementCursor.next != null) {
            logger.debug { "child cursor ${childElementCursor.localName}" }
        }
    }
}
