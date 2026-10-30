package io.github.bayang.jelu.service

import io.github.bayang.jelu.bookDto
import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.controllers.DirectoryRequestDto
import io.github.bayang.jelu.controllers.FileSystemController
import io.github.bayang.jelu.controllers.PICTURES
import io.github.bayang.jelu.dto.BookDto
import io.github.bayang.jelu.errors.JeluValidationException
import io.github.bayang.jelu.service.metadata.FileMetadataService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.io.File
import java.nio.file.Files

@SpringBootTest
class ServerFileAccessTest(
    @Autowired private val bookService: BookService,
    @Autowired private val jeluProperties: JeluProperties,
    @Autowired private val fileSystemController: FileSystemController,
    @Autowired private val fileMetadataService: FileMetadataService,
) {
    @TempDir
    lateinit var imagesDir: File

    @TempDir
    lateinit var importsDir: File

    private lateinit var previousImages: String
    private lateinit var previousImports: String
    private val created = mutableListOf<BookDto>()

    @BeforeEach
    fun setup() {
        previousImages = jeluProperties.files.images
        previousImports = jeluProperties.files.imports
        jeluProperties.files.images = imagesDir.absolutePath
        jeluProperties.files.imports = importsDir.absolutePath
    }

    @AfterEach
    fun cleanup() {
        created.forEach { bookService.deleteBookById(it.id!!) }
        created.clear()
        jeluProperties.files.images = previousImages
        jeluProperties.files.imports = previousImports
        jeluProperties.files.allowedDirs = emptyList()
    }

    private fun list(path: String) = fileSystemController.getDirectoryListing(DirectoryRequestDto(path = path, reason = PICTURES))

    private fun assertForbidden(path: String) {
        val e = Assertions.assertThrows(ResponseStatusException::class.java) { list(path) }
        Assertions.assertEquals(HttpStatus.FORBIDDEN, e.statusCode)
    }

    private fun saveWithImage(image: String): BookDto =
        bookService.save(bookDto(title = "server file access").copy(image = image, authors = mutableListOf()), null).also { created += it }

    @Test
    fun `a book image pointing outside the allowed dirs is not copied into the images dir`() {
        // Any file the server process can read, here this repo's own build file. Before the fix
        // saveImages copied it into the images dir, where /files/** serves it without auth.
        val outside = File("build.gradle.kts").absoluteFile
        Assertions.assertTrue(outside.isFile)

        val res = saveWithImage(outside.absolutePath)

        Assertions.assertNull(res.image)
        Assertions.assertEquals(0, imagesDir.listFiles()!!.size)
    }

    @Test
    fun `a book image picked inside the imports dir is still copied`() {
        val cover = File(importsDir, "cover.jpg")
        this::class.java.getResourceAsStream("/test-cover.jpg")!!.use { input -> cover.outputStream().use { input.copyTo(it) } }

        val res = saveWithImage(cover.absolutePath)

        Assertions.assertNotNull(res.image)
        Assertions.assertEquals(1, imagesDir.listFiles()!!.size)
    }

    @Test
    fun `the picker starts from the allowed dirs instead of the filesystem roots`() {
        val second = File(imagesDir, "library").apply { mkdirs() }
        jeluProperties.files.allowedDirs = listOf(importsDir.absolutePath, second.absolutePath)

        val roots = list("").directories.map { it.path }

        Assertions.assertEquals(listOf(importsDir.toPath().toRealPath().toString(), second.toPath().toRealPath().toString()), roots)
    }

    @Test
    fun `browsing is limited to the allowed dirs`() {
        File(importsDir, "sub").mkdirs()
        val inside = list(File(importsDir, "sub").absolutePath)
        Assertions.assertEquals(importsDir.toPath().toRealPath().toString(), inside.parent)
        // going up from an allowed dir leads back to the starting points
        Assertions.assertEquals("", list(importsDir.absolutePath).parent)

        assertForbidden(File("build.gradle.kts").absoluteFile.parentFile.absolutePath)
        assertForbidden(importsDir.absolutePath + File.separator + "..")
    }

    @Test
    fun `a symlink inside an allowed dir cannot be used to leave it`() {
        val link = File(importsDir, "escape").toPath()
        try {
            Files.createSymbolicLink(link, File("build.gradle.kts").absoluteFile.parentFile.toPath())
        } catch (e: Exception) {
            Assumptions.abort<Unit>("cannot create symlinks here: ${e.message}")
        }
        assertForbidden(link.toString())
    }

    @Test
    fun `metadata is only read from a file inside the allowed dirs`() {
        val outside = File("build.gradle.kts").absoluteFile
        Assertions.assertThrows(JeluValidationException::class.java) { fileMetadataService.extractMetadata(outside.absolutePath) }

        val opf = File(importsDir, "book.opf")
        this::class.java.getResourceAsStream("/metadata/content.opf")!!.use { input -> opf.outputStream().use { input.copyTo(it) } }
        Assertions.assertNotNull(fileMetadataService.extractMetadata(opf.absolutePath))
    }
}
