package io.github.bayang.jelu.controllers

import com.fasterxml.jackson.annotation.JsonInclude
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.streams.asSequence

const val METADATA = "metadata"
const val PICTURES = "pictures"

private val logger = KotlinLogging.logger {}

// see https://github.com/gotson/komga/blob/master/komga/src/main/kotlin/org/gotson/komga/interfaces/api/rest/FileSystemController.kt
@RestController
@RequestMapping("api/v1/filesystem", produces = [MediaType.APPLICATION_JSON_VALUE])
class FileSystemController {

    private val fs = FileSystems.getDefault()

    @PostMapping
    fun getDirectoryListing(
        @RequestBody(required = false) request: DirectoryRequestDto = DirectoryRequestDto(),
    ): DirectoryListingDto =
        if (request.reason != METADATA && request.reason != PICTURES) {
            logger.error { "unknown reason ${request.reason}" }
            DirectoryListingDto(
                directories = listOf(),
            )
        } else if (request.path.isEmpty()) {
            DirectoryListingDto(
                directories = fs.rootDirectories.map { it.toDto() },
            )
        } else {
            val p = fs.getPath(request.path)
            if (!p.isAbsolute) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Path must be absolute")
            try {
                DirectoryListingDto(
                    parent = (p.parent ?: "").toString(),
                    directories = Files.list(p).use { dirStream ->
                        dirStream.asSequence()
                            .filter { !Files.isHidden(it) }
                            .filter { filterReason(it, request.reason) }
                            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.toString() })
                            .map { it.toDto() }
                            .toList()
                    },
                )
            } catch (e: Exception) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Path does not exist")
            }
        }
}

fun filterReason(p: Path, reason: String): Boolean {
    if (reason == METADATA) {
        return isSupportedExtension(p)
    }
    return p.isDirectory() ||
        (
            p.name.endsWith(".jpg", true) ||
                p.name.endsWith(".jpeg", true) ||
                p.name.endsWith(".png", true)
            )
}

fun isSupportedExtension(p: Path): Boolean {
    return p.isDirectory() || (
        p.name.endsWith(".epub", true) ||
            p.name.endsWith(".opf", true)
        )
}

data class DirectoryRequestDto(
    val path: String = "",
    val reason: String = METADATA,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DirectoryListingDto(
    val parent: String? = null,
    val directories: List<PathDto>,
)

data class PathDto(
    val type: String,
    val name: String,
    val path: String,
)

fun Path.toDto(): PathDto =
    PathDto(
        type = if (Files.isDirectory(this)) "directory" else "file",
        name = (fileName ?: this).toString(),
        path = toString(),
    )
