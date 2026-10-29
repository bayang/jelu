package io.github.bayang.jelu.service

import io.github.bayang.jelu.config.JeluProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

/**
 * The directories users may read from on the server: browsing in the file picker, importing
 * metadata from a server file and picking a cover from the server all go through here.
 * Configured with jelu.files.allowed-dirs, and falls back to the imports dir when that is empty.
 */
@Component
class ServerFileAccess(
    private val properties: JeluProperties,
) {
    fun roots(): List<Path> {
        val configured = properties.files.allowedDirs.filter { it.isNotBlank() }
        return configured.ifEmpty { listOf(properties.files.imports) }.mapNotNull { realDir(it) }.distinct()
    }

    /**
     * [path] resolved with symlinks and .. segments followed, or null when it is not absolute,
     * does not exist, or ends up outside every allowed dir.
     */
    fun resolve(path: String?): Path? {
        if (path.isNullOrBlank()) return null
        val real =
            try {
                val p = Path.of(path)
                if (!p.isAbsolute) return null
                p.toRealPath()
            } catch (e: Exception) {
                return null
            }
        return real.takeIf { r -> roots().any { r.startsWith(it) } }
    }

    fun isRoot(path: Path): Boolean = roots().any { it == path }

    private fun realDir(dir: String): Path? =
        try {
            Path.of(dir).toRealPath().takeIf { Files.isDirectory(it) }
        } catch (e: Exception) {
            logger.warn { "allowed dir $dir is not usable: ${e.message}" }
            null
        }
}
