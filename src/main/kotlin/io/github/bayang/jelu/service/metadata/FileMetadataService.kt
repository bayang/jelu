package io.github.bayang.jelu.service.metadata

import io.github.bayang.jelu.dto.MetadataDto
import io.github.bayang.jelu.errors.JeluException
import io.github.bayang.jelu.errors.JeluValidationException
import mu.KotlinLogging
import org.jsoup.Jsoup
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedReader
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.deleteIfExists

private val logger = KotlinLogging.logger {}

@Service
class FileMetadataService(private val opfParser: OpfParser) {

    fun extractMetadata(filePath: String): MetadataDto? {
        val file = File(filePath)
        if (!file.exists()) {
            throw JeluValidationException("file does not exist")
        }
        if (filePath.endsWith("epub", true)) {
            ZipFile(file).use { zip ->
                try {
                    val opfFile = getPackagePath(zip)
                    val metadata = zip.getInputStream(zip.getEntry(opfFile)).use {
                        opfParser.parseOpf(it.bufferedReader().use(BufferedReader::readText))
                    }
                    return metadata
                } catch (e: Exception) {
                    logger.error(e) { "Failed to parse epub" }
                }
            }
        } else if (filePath.endsWith("opf", true)) {
            return opfParser.parseOpf(file.inputStream().bufferedReader().use(BufferedReader::readText))
        }
        return null
    }

    fun extractMetadata(file: MultipartFile): MetadataDto? {
        if (file.originalFilename.isNullOrBlank()) {
            return null
        }
        val name = file.originalFilename
        if (name != null) {
            if (name.endsWith("epub", true)) {
                var tempFile: Path? = null
                try {
                    tempFile = Files.createTempFile("jelu", null)
                    file.transferTo(tempFile)
                    ZipFile(tempFile.toFile()).use { zip ->
                        val opfFile = getPackagePath(zip)
                        val metadata = zip.getInputStream(zip.getEntry(opfFile)).use {
                            opfParser.parseOpf(it.bufferedReader().use(BufferedReader::readText))
                        }
                        return metadata
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Failed to parse epub" }
                } finally {
                    try {
                        tempFile?.deleteIfExists()
                    } catch (e: Exception) {
                        logger.error(e) { "Failed to delete temp file" }
                    }
                }
            } else if (name.endsWith("opf", true)) {
                return opfParser.parseOpf(file.inputStream.bufferedReader().use(BufferedReader::readText))
            }
        }
        return null
    }

    private fun getPackagePath(zip: ZipFile): String =
        zip.getEntry("META-INF/container.xml").let { entry ->
            val container = zip.getInputStream(entry).use { Jsoup.parse(it, null, "") }
            container.getElementsByTag("rootfile").first()?.attr("full-path")
                ?: throw JeluException("META-INF/container.xml does not contain rootfile tag")
        }
}
