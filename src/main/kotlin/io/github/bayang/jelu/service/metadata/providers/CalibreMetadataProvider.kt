package io.github.bayang.jelu.service.metadata.providers

import com.ctc.wstx.stax.WstxInputFactory
import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dto.MetadataDto
import io.github.bayang.jelu.dto.MetadataRequestDto
import io.github.bayang.jelu.service.metadata.OpfParser
import io.github.bayang.jelu.service.metadata.PluginInfoHolder
import io.github.bayang.jelu.utils.slugify
import mu.KotlinLogging
import org.apache.commons.validator.routines.ISBNValidator
import org.codehaus.staxmate.SMInputFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.io.BufferedReader
import java.io.File

private val logger = KotlinLogging.logger {}

@Service
class CalibreMetadataProvider(
    private val properties: JeluProperties,
    private val opfParser: OpfParser,
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
        config: Map<String, String>,
    ): Mono<MetadataDto>? {
        if (metadataRequestDto.isbn.isNullOrBlank() && metadataRequestDto.title.isNullOrBlank() &&
            metadataRequestDto.authors.isNullOrBlank()
        ) {
            logger.error { "At least one of isbn, authors or title is required to fetch metadata" }
            return null
        }
        val onlyUseCorePlugins: Boolean = if (config.containsKey(CalibreMetadataProvider.onlyUseCorePlugins)) {
            config[CalibreMetadataProvider.onlyUseCorePlugins].toBoolean()
        } else {
            false
        }
        val fetchCover: Boolean = if (config.containsKey(CalibreMetadataProvider.fetchCover)) {
            config[CalibreMetadataProvider.fetchCover].toBoolean()
        } else {
            true
        }
        var bookFileName: String = FILE_PREFIX
        val commandArray: MutableList<String> = mutableListOf(properties.metadata.calibre.path!!, "-o", "-d 90")
        var fileNameComplete = false
        if (!metadataRequestDto.isbn.isNullOrBlank()) {
            bookFileName += metadataRequestDto.isbn
            fileNameComplete = true
            if (determineCodeType(metadataRequestDto.isbn) == "ASIN") {
                commandArray.add("-I")
                commandArray.add("asin:" + metadataRequestDto.isbn)
            } else {
                commandArray.add("-i")
                commandArray.add(metadataRequestDto.isbn)
            }
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
                var output: String = process.inputStream.bufferedReader().use(BufferedReader::readText)
                // on ARM the fetch-ebook-metadata binary outputs a python byte string instead of a regular string
                // cf test case for a sample string
                // so we try to clean it ourselves... This is ugly
                if (!output.startsWith('<')) {
                    logger.trace { "fetch metadata output is not regular xml : $output" }
                    output = cleanXml(output)
                }
                logger.trace { "fetch metadata output $output" }
                val parseOpf: MetadataDto = opfParser.parseOpf(output)
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

    internal fun determineCodeType(code: String): String {
        // Regex for ISBN-10: Either 10 digits, or 9 digits followed by 'X'
        val isbn10Regex = Regex("^(?=[0-9X]{10}\$)([0-9]{9}[X]|[0-9]{10})\$")

        // Regex for ISBN-13: Starts with 978 or 979 followed by 10 digits
        val isbn13Regex = Regex("^(978|979)[0-9]{10}\$")

        // Regex for ASIN: 10 alphanumeric characters
        val asinRegex = Regex("^[A-Z0-9]{10}\$")

        return when {
            // Strip all hyphens and spaces from the code before matching
            isbn10Regex.matches(code.replace("-", "").replace(" ", "")) -> "ISBN-10"
            isbn13Regex.matches(code.replace("-", "").replace(" ", "")) -> "ISBN-13"
            asinRegex.matches(code) -> "ASIN"
            else -> "Unknown format"
        }
    }
}
