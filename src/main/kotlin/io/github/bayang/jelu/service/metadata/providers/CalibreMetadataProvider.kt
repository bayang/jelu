package io.github.bayang.jelu.service.metadata.providers

import com.ctc.wstx.stax.WstxInputFactory
import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dto.MetadataDto
import io.github.bayang.jelu.dto.MetadataRequestDto
import io.github.bayang.jelu.service.metadata.CREATOR
import io.github.bayang.jelu.service.metadata.DATE
import io.github.bayang.jelu.service.metadata.DESCRIPTION
import io.github.bayang.jelu.service.metadata.GUIDE
import io.github.bayang.jelu.service.metadata.IDENTIFIER
import io.github.bayang.jelu.service.metadata.LANGUAGE
import io.github.bayang.jelu.service.metadata.META
import io.github.bayang.jelu.service.metadata.METADATA
import io.github.bayang.jelu.service.metadata.PUBLISHER
import io.github.bayang.jelu.service.metadata.PluginInfoHolder
import io.github.bayang.jelu.service.metadata.SUBJECT
import io.github.bayang.jelu.service.metadata.TITLE
import io.github.bayang.jelu.utils.sanitizeHtml
import io.github.bayang.jelu.utils.slugify
import mu.KotlinLogging
import org.apache.commons.validator.routines.ISBNValidator
import org.codehaus.staxmate.SMInputFactory
import org.codehaus.staxmate.`in`.SMHierarchicCursor
import org.codehaus.staxmate.`in`.SMInputCursor
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File

private val logger = KotlinLogging.logger {}

@Service
class CalibreMetadataProvider(
    private val properties: JeluProperties,
) : IMetaDataProvider {

    companion object {
        const val fetchCover = "fetchCover"
        const val onlyUseCorePlugins = "onlyUseCorePlugins"
        const val FILE_PREFIX = "meta-import-"
    }

    val factory: SMInputFactory = SMInputFactory(WstxInputFactory())
    private val validator: ISBNValidator = ISBNValidator.getInstance(false)

    override fun fetchMetadata(
        metadataRequestDto: MetadataRequestDto,
        config: Map<String, String>
    ): Mono<MetadataDto>? {
        if (metadataRequestDto.isbn.isNullOrBlank() && metadataRequestDto.title.isNullOrBlank() &&
            metadataRequestDto.authors.isNullOrBlank()
        ) {
            logger.error { "At least one of isbn, authors or title is required to fetch metadata" }
            return null
        }
        val onlyUseCorePlugins: Boolean = if (config.containsKey(CalibreMetadataProvider.onlyUseCorePlugins))
            config[CalibreMetadataProvider.onlyUseCorePlugins].toBoolean() else false
        val fetchCover: Boolean = if (config.containsKey(CalibreMetadataProvider.fetchCover))
            config[CalibreMetadataProvider.fetchCover].toBoolean() else true
        var bookFileName: String = FILE_PREFIX
        val commandArray: MutableList<String> = mutableListOf(properties.metadata.calibre.path!!, "-o", "-d 90")
        var fileNameComplete = false
        if (!metadataRequestDto.isbn.isNullOrBlank()) {
            bookFileName += metadataRequestDto.isbn
            fileNameComplete = true
            commandArray.add("-i")
            commandArray.add(metadataRequestDto.isbn)
        }
        if (!metadataRequestDto.title.isNullOrBlank()) {
            commandArray.add("-t")
            commandArray.add(metadataRequestDto.title)
            if (!fileNameComplete) {
                bookFileName += slugify(metadataRequestDto.title)
                fileNameComplete = true
            }
        }
        if (!metadataRequestDto.authors.isNullOrBlank()) {
            commandArray.add("-a")
            commandArray.add(metadataRequestDto.authors)
            if (!fileNameComplete) {
                bookFileName += slugify(metadataRequestDto.authors)
                fileNameComplete = true
            }
        }
        if (onlyUseCorePlugins) {
            commandArray.add("-p")
            commandArray.add("Google")
            commandArray.add("-p")
            commandArray.add("Amazon.com")
        }
        // add a bit of randomness to prevent images with same names.
        // Otherwise different files with same names are cached, see issue #41
        val now = System.currentTimeMillis()
        bookFileName += "-$now.jpg"
        val targetCover = File(properties.files.images, bookFileName)
        if (targetCover != null && targetCover.exists()) {
            try {
                val deleted = targetCover.delete()
                logger.trace { "deleted already existing cover temporary image ${targetCover.absolutePath}" }
            } catch (e: Exception) {
                logger.error(e) { "failed to delete image ${targetCover.absolutePath}" }
            }
        }
        if (fetchCover) {
            commandArray.add("-c")
            commandArray.add(targetCover.absolutePath)
        }
        val builder = ProcessBuilder()
        logger.trace { "fetch metadata command : $commandArray" }
        builder.command(commandArray)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
        try {
            val process: Process = builder.start()
            val exitVal = process.waitFor()
            if (exitVal == 0) {
                var output: String = process.inputStream.bufferedReader().readText()
                // on ARM the fetch-ebook-metadata binary outputs a python byte string instead of a regular string
                // cf test case for a sample string
                // so we try to clean it ourselves... This is ugly
                if (!output.startsWith('<')) {
                    logger.trace { "fetch metadata output is not regular xml : $output" }
                    output = cleanXml(output)
                }
                logger.trace { "fetch metadata output $output" }
                val parseOpf: MetadataDto = parseOpf(output)
                if (!metadataRequestDto.isbn.isNullOrBlank()) {
                    if (validator.isValidISBN13(metadataRequestDto.isbn) && parseOpf.isbn13.isNullOrBlank()) {
                        parseOpf.isbn13 = metadataRequestDto.isbn
                    } else if (validator.isValidISBN10(metadataRequestDto.isbn) && parseOpf.isbn10.isNullOrBlank()) {
                        parseOpf.isbn10 = metadataRequestDto.isbn
                    }
                }
                if (targetCover.exists() && targetCover.length() > 0) {
                    parseOpf.image = targetCover.name
                    logger.trace { "fetch metadata image ${targetCover.name}" }
                }
                return Mono.just(parseOpf)
            } else {
                logger.error { "fetch ebookmetadata process exited abnormally with code $exitVal" }
                return null
            }
        } catch (e: Exception) {
            logger.error(e) { "failure while calling fetch-ebook-metadata process" }
            return null
        }
    }

    override fun name(): String {
        return PluginInfoHolder.calibre
    }

    private fun removeTrailingAndLeadingChars(output: String): String {
        if (output.isNullOrBlank()) {
            return output
        }
        var startIndex = 0
        var endIndex = output.length - 1
        while (output[startIndex] != '<') {
            startIndex++
        }
        while (output[endIndex] != '>') {
            endIndex--
        }
        return output.substring(startIndex, endIndex + 1)
    }

    internal fun cleanXml(input: String): String {
        var trimmed = removeTrailingAndLeadingChars(input)
        if (trimmed.contains("\\'")) {
            trimmed = trimmed.replace("\\'", "'")
        }
        if (trimmed.contains("\\n")) {
            trimmed = trimmed.replace("\\n", "")
        }
        return trimmed
    }

    internal fun parseOpf(input: String): MetadataDto {
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
                        logger.trace { "rootChildren name ${rootChildrenCursor.localName} qname ${rootChildrenCursor.qName}" }
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

                        else -> logger.trace { "unhandled identifier scheme ${childElementCursor.getAttrValue("scheme")}" }
                    }
                }

                TITLE -> dto.title = childElementCursor.elemStringValue
                CREATOR -> {
                    when (childElementCursor.getAttrValue("role")) {
                        // sometimes we receive mutliple authors on one line, separated by ;
                        "aut" -> dto.authors.addAll(splitValues(childElementCursor.elemStringValue))
                        else -> logger.trace { "unhandled creator role ${childElementCursor.getAttrValue("role")}" }
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
            logger.trace { "child cursor ${childElementCursor.localName}" }
        }
    }
}
