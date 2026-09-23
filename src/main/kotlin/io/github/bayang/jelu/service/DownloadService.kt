package io.github.bayang.jelu.service

import io.github.bayang.jelu.utils.imageName
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.io.FilenameUtils
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels

private val logger = KotlinLogging.logger {}

// A cover host that accepts the connection and then stalls would otherwise block the
// calling thread forever, so we bound both the connect and the read phase
private const val CONNECT_TIMEOUT_MS = 10_000
private const val READ_TIMEOUT_MS = 30_000

@Service
class DownloadService {
    fun download(
        sourceUrl: String,
        title: String,
        bookId: String,
        targetFolder: String,
    ): String {
        try {
            val url: URL = URL(sourceUrl)
            logger.debug { "path ${url.path} file ${url.file}" }
            val conn = url.openConnection()
            conn.setRequestProperty("User-Agent", "jelu-app")
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS
            val filename: String = imageName(title, bookId, FilenameUtils.getExtension(url.path))
            val targetFile: File = File(targetFolder, filename)
            conn.getInputStream().use { stream ->
                Channels.newChannel(stream).use { readableByteChannel ->
                    FileOutputStream(targetFile).use { fileOutputStream ->
                        fileOutputStream.channel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE)
                    }
                }
            }
            return filename
        } catch (e: Exception) {
            logger.error("failed to download file from $sourceUrl", e)
            throw e
        }
    }
}
