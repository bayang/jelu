package io.github.bayang.jelu.service

import io.github.bayang.jelu.utils.imageName
import mu.KotlinLogging
import org.apache.commons.io.FilenameUtils
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.channels.ReadableByteChannel

private val logger = KotlinLogging.logger {}

@Service
class DownloadService {

    fun download(sourceUrl: String, title: String, bookId: String, targetFolder: String): String {
        try {
            val url: URL = URL(sourceUrl)
            logger.debug{ "path ${url.path} file ${url.file}" }
            var readableByteChannel: ReadableByteChannel = Channels.newChannel(url.openStream())
            val filename: String = imageName(title, bookId, FilenameUtils.getExtension(url.path))
            val targetFile: File = File(targetFolder, filename)
            val fileOutputStream: FileOutputStream = FileOutputStream(targetFile)
            val channel: FileChannel = fileOutputStream.channel
            channel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE)
            return filename
        }
        catch (e:Exception) {
            logger.error ("failed to download file from $sourceUrl", e)
            throw e
        }
    }
}