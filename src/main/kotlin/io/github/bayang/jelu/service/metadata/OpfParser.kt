package io.github.bayang.jelu.service.metadata

import com.ctc.wstx.stax.WstxInputFactory
import io.github.bayang.jelu.dto.MetadataDto
import io.github.bayang.jelu.utils.sanitizeHtml
import mu.KotlinLogging
import org.apache.commons.validator.routines.ISBNValidator
import org.codehaus.staxmate.SMInputFactory
import org.codehaus.staxmate.`in`.SMHierarchicCursor
import org.codehaus.staxmate.`in`.SMInputCursor
import org.springframework.stereotype.Service
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream

private val logger = KotlinLogging.logger {}

@Service
class OpfParser {

    val factory: SMInputFactory = SMInputFactory(WstxInputFactory())
    private val validator: ISBNValidator = ISBNValidator.getInstance(false)

    fun parseOpf(input: String): MetadataDto {
        val stream = BufferedInputStream(ByteArrayInputStream(input.toByteArray(Charsets.UTF_8)))
        val root: SMHierarchicCursor = factory.rootElementCursor(stream)
        val dto = MetadataDto()
        val tempData = mutableMapOf<String, String>()
        try {
            root.advance()
            val rootChildrenCursor = root.childElementCursor()
            while (rootChildrenCursor.next != null) {
                when (rootChildrenCursor.localName) {
                    METADATA -> metadata(rootChildrenCursor.childElementCursor(), dto, tempData)
                    GUIDE -> logPart(rootChildrenCursor.childElementCursor())
                    else -> {
                        logger.trace { "rootChildren name ${rootChildrenCursor.localName} qname ${rootChildrenCursor.qName}" }
                        logPart(rootChildrenCursor.childElementCursor())
                    }
                }
            }
            logger.debug { "parsed dto $dto" }
            enrichMetadata(dto, tempData)
        } catch (e: Exception) {
            logger.error(e) { "failure while parsing opf metadata from calibre" }
        } finally {
            root.streamReader.closeCompletely()
        }
        return dto
    }

    private fun enrichMetadata(dto: MetadataDto, tempData: MutableMap<String, String>) {
        if (dto.isbn10.isNullOrBlank() && tempData.containsKey("isbn10")) {
            dto.isbn10 = tempData["isbn10"]
        }
        if (dto.isbn13.isNullOrBlank() && tempData.containsKey("isbn13")) {
            dto.isbn13 = tempData["isbn13"]
        }
        if (dto.series.isNullOrBlank() && tempData.containsKey("series")) {
            dto.series = tempData["series"]
        }
        if (dto.numberInSeries == null && tempData.containsKey("numberInSeries")) {
            dto.numberInSeries = tempData["numberInSeries"]?.toDoubleOrNull()
        }
    }

    private fun metadata(childElementCursor: SMInputCursor, dto: MetadataDto, tempData: MutableMap<String, String>) {
        while (childElementCursor.next != null) {
            when (childElementCursor.localName) {
                IDENTIFIER -> {
                    when (childElementCursor.getAttrValue("scheme")) {
                        "GOODREADS" -> dto.goodreadsId = childElementCursor.elemStringValue
                        "GOOGLE" -> dto.googleId = childElementCursor.elemStringValue
                        "AMAZON" -> dto.amazonId = childElementCursor.elemStringValue
                        "ISBN" -> {
                            val isbn = childElementCursor.elemStringValue.lowercase().removePrefix("urn:").removePrefix("isbn:")
                            if (validator.isValidISBN13(isbn)) {
                                dto.isbn13 = isbn
                            } else if (validator.isValidISBN10(isbn)) {
                                dto.isbn10 = isbn
                            }
                        }
                        else -> {
                            logger.trace { "unhandled identifier scheme ${childElementCursor.getAttrValue("scheme")}" }
                            if (childElementCursor.getAttrValue("scheme").isNullOrBlank()) {
                                val identifier = childElementCursor.elemStringValue
                                // some identifiers are case sensitive, like pPamZwEACAAJ for google
                                if (identifier.isNotBlank()) {
                                    if (identifier.startsWith("isbn:", true)) {
                                        val isbn = identifier.removePrefix("isbn:").removePrefix("ISBN:")
                                        if (validator.isValidISBN13(isbn)) {
                                            dto.isbn13 = isbn
                                        } else if (validator.isValidISBN10(isbn)) {
                                            dto.isbn10 = isbn
                                        }
                                    } else if (identifier.startsWith("goodreads:", true)) {
                                        dto.goodreadsId = identifier.removePrefix("goodreads:").removePrefix("GOODREADS:")
                                    } else if (identifier.startsWith("google:", true)) {
                                        dto.googleId = identifier.removePrefix("google:").removePrefix("GOOGLE:")
                                    } else if (identifier.startsWith("amazon:", true)) {
                                        dto.amazonId = identifier.removePrefix("amazon:").removePrefix("AMAZON:")
                                    }
                                }
                            }
                        }
                    }
                }
                SOURCE -> {
                    val value = childElementCursor.elemStringValue
                    if (!value.isNullOrBlank() && value.contains("isbn", true)) {
                        val isbn = value.lowercase().removePrefix("urn:").removePrefix("isbn:")
                        if (validator.isValidISBN13(isbn)) {
                            tempData["isbn13"] = isbn
                        } else if (validator.isValidISBN10(isbn)) {
                            tempData["isbn10"] = isbn
                        }
                    }
                }
                TITLE -> dto.title = childElementCursor.elemStringValue
                CREATOR -> {
                    when (childElementCursor.getAttrValue("role")) {
                        // sometimes we receive mutliple authors on one line, separated by ;
                        "aut" -> dto.authors.addAll(splitValues(childElementCursor.elemStringValue))
                        else -> {
                            logger.trace { "unhandled creator role ${childElementCursor.getAttrValue("role")}" }
                            dto.authors.addAll(splitValues(childElementCursor.elemStringValue))
                        }
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
                        "calibre:series_index" ->
                            dto.numberInSeries =
                                childElementCursor.getAttrValue("content").toDoubleOrNull()

                        "calibre:author_link_map" -> logger.trace { "author_link_map ${childElementCursor.getAttrValue("content")}" }
                        else -> logger.trace { "unhandled meta name ${childElementCursor.getAttrValue("name")}" }
                    }
                    when (childElementCursor.getAttrValue("property")) {
                        "belongs-to-collection" -> tempData["series"] = childElementCursor.elemStringValue
                        "group-position" ->
                            tempData["numberInSeries"] =
                                childElementCursor.elemStringValue
                        else -> logger.trace { "unhandled meta name ${childElementCursor.getAttrValue("property")}" }
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
                elemStringValue.split(";").filter { it.isNotBlank() }.map { it.trim() }.toSet()
            } else {
                setOf(elemStringValue.trim())
            }
        }
    }

    private fun logPart(childElementCursor: SMInputCursor) {
        while (childElementCursor.next != null) {
            logger.trace { "child cursor ${childElementCursor.localName}" }
        }
    }
}
