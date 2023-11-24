package io.github.bayang.jelu.service.metadata

import io.github.bayang.jelu.errors.JeluValidationException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File

@SpringBootTest
class FileMetadataServiceTest(@Autowired private val fileMetadataService: FileMetadataService) {

    @Test
    fun testParseEpub() {
        val metadata =
            fileMetadataService.extractMetadata(File(this::class.java.getResource("/metadata/pg72155-images.epub").file).absolutePath)
        Assertions.assertEquals("Travels in Southern Abyssinia, Volume I (of 2)", metadata?.title)
        Assertions.assertEquals(1, metadata?.authors?.size)
        Assertions.assertEquals(0, metadata?.tags?.size)
        Assertions.assertEquals("en", metadata?.language)
    }

    @Test
    fun testParseEpub3() {
        val metadata =
            fileMetadataService.extractMetadata(File(this::class.java.getResource("/metadata/pg72155-images-3.epub").file).absolutePath)
        Assertions.assertEquals("Travels in Southern Abyssinia, Volume I (of 2)", metadata?.title)
        Assertions.assertEquals(1, metadata?.authors?.size)
        Assertions.assertEquals(0, metadata?.tags?.size)
        Assertions.assertEquals("en", metadata?.language)
    }

    @Test
    fun testParseOpfNoRole() {
        val metadata = fileMetadataService.extractMetadata(File(this::class.java.getResource("/metadata/content.opf").file).absolutePath)
        Assertions.assertNull(metadata?.googleId)
        Assertions.assertEquals("9782370491190", metadata?.isbn13)
        Assertions.assertNull(metadata?.isbn10)
        Assertions.assertEquals(1, metadata?.authors?.size)
        Assertions.assertEquals(1, metadata?.tags?.size)
    }

    @Test
    fun testParseUnknownExtension() {
        assertThrows<JeluValidationException> { fileMetadataService.extractMetadata("unknown.txt") }
    }
}
