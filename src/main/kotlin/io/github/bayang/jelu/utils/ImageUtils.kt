package io.github.bayang.jelu.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.name.Rename
import java.io.File

private val logger = KotlinLogging.logger {}

fun resizeImage(originalFile: File) {
    var backup: File? = null
    try {
        backup = originalFile.copyTo(File("${originalFile.absolutePath}.bak"), true)
        Thumbnails
            .of(originalFile)
            .allowOverwrite(true)
            .useOriginalFormat()
            .size(500, 500)
            .keepAspectRatio(true)
            .toFiles(Rename.NO_CHANGE)
        try {
            backup.delete()
        } catch (e: Exception) {
            // noop
        }
    } catch (e: Exception) {
        logger.error(e) { "Failed to resize image ${originalFile.name}" }
        if (backup != null && backup.exists()) {
            backup.copyTo(File(originalFile.absolutePath), true)
        }
    }
}
