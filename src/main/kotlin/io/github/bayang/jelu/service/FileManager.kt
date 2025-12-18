package io.github.bayang.jelu.service

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.service.metadata.providers.CalibreMetadataProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.io.path.deleteIfExists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.name

private val logger = KotlinLogging.logger {}

@Component
class FileManager(
    private val jeluProperties: JeluProperties,
) {

    @Scheduled(cron = "\${files.metadataImportCleanCron:0 0 */6 * * *}")
    fun cleanMetadataImportFiles() {
        doCleanMetadataImportFiles()
    }

    @Scheduled(cron = "\${files.bakFilesCleanCron:0 30 */6 * * *}")
    fun cleanTemporaryImageFiles() {
        doCleanTemporaryImageFiles()
    }

    /**
     *   Delete temporary covers from resizing or cover update
     */
    fun doCleanTemporaryImageFiles() {
        val now = OffsetDateTime.now(ZoneId.systemDefault()).toInstant()
        Files.walk(Paths.get(jeluProperties.files.images)).use {
                paths ->
            paths.filter {
                it.name.endsWith(".bak", true) &&
                    it.getLastModifiedTime() != null &&
                    it.getLastModifiedTime().toInstant().plus(4, ChronoUnit.DAYS).isBefore(now)
            }
                .forEach { deletePath(it) }
        }
    }

    /**
     *   Delete leftover covers from automatic metadata importing
     */
    fun doCleanMetadataImportFiles() {
        val now = OffsetDateTime.now(ZoneId.systemDefault()).toInstant()
        Files.walk(Paths.get(jeluProperties.files.images)).use {
                paths ->
            paths.filter {
                it.name.startsWith(CalibreMetadataProvider.FILE_PREFIX, true) &&
                    it.getLastModifiedTime() != null &&
                    it.getLastModifiedTime().toInstant().plus(1, ChronoUnit.HOURS).isBefore(now)
            }
                .forEach { deletePath(it) }
        }
    }

    fun deletePath(path: Path): Boolean {
        try {
            logger.trace { "deleting ${path.name}" }
            return path.deleteIfExists()
        } catch (e: Exception) {
            logger.error(e) { "failed to delete image $path" }
        }
        return false
    }

    fun deleteImage(path: String): Boolean {
        try {
            return deletePath(Paths.get(jeluProperties.files.images, path))
        } catch (e: Exception) {
            logger.error(e) { "failed to delete image $path" }
        }
        return false
    }
}
