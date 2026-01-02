package io.github.bayang.jelu.service.metadata.providers

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dto.MetadataDto
import io.github.bayang.jelu.dto.MetadataError
import io.github.bayang.jelu.dto.MetadataRequestDto
import io.github.bayang.jelu.service.metadata.OpfParser
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException

@SpringBootTest
@TestPropertySource(
    properties =
        [
            "jelu.metadata.calibre.path=/fake/calibre/path",
            "jelu.metadata.calibre.timeout=30",
            "jelu.files.images=/tmp/test-images",
        ],
)
class CalibreMetadataProviderTest(
    @Autowired private val opfParser: OpfParser,
) {
    private lateinit var calibreMetadataProvider: CalibreMetadataProvider
    private lateinit var jeluProperties: JeluProperties
    private lateinit var processBuilderFactory: ProcessBuilderFactory

    @BeforeEach
    fun setUp() {
        // opfParser = mockk<OpfParser>()
        jeluProperties = mockk<JeluProperties>()
        processBuilderFactory = mockk<ProcessBuilderFactory>()

        val calibreConfig = mockk<JeluProperties.Calibre>()
        every { calibreConfig.path } returns "/fake/calibre/path"
        every { calibreConfig.timeout } returns 30

        val metadataConfig = mockk<JeluProperties.Metadata>()
        every { metadataConfig.calibre } returns calibreConfig

        val filesConfig = mockk<JeluProperties.Files>()
        every { filesConfig.images } returns "/tmp/test-images"

        every { jeluProperties.metadata } returns metadataConfig
        every { jeluProperties.files } returns filesConfig

        calibreMetadataProvider =
            CalibreMetadataProvider(jeluProperties, opfParser, processBuilderFactory)
    }

    private fun createBasicXmlOutput(
        title: String = "The Fellowship of the Ring",
        author: String = "J. R. R. Tolkien",
        identifierScheme: String = "ISBN",
        identifier: String = "9780547928210",
    ): String =
        """<?xml version='1.0' encoding='utf-8'?>
            <package xmlns="http://www.idpf.org/2007/opf" unique-identifier="uuid_id" version="2.0">
                <metadata xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:opf="http://www.idpf.org/2007/opf">
                    <dc:title>$title</dc:title>
                    <dc:creator opf:role="aut">$author</dc:creator>
                    <dc:identifier opf:scheme="$identifierScheme">$identifier</dc:identifier>
                </metadata>
            </package>"""

    @Test
    fun testFetchMetadataWithEmptyRequest() {
        // Test that empty request returns Optional.empty()
        val metadataRequest = MetadataRequestDto()
        val config = mapOf<String, String>()

        val result = calibreMetadataProvider.fetchMetadata(metadataRequest, config)

        Assertions.assertFalse(result.isPresent)
    }

    @Test
    fun testFetchMetadataWithExitCodeNotZero() {
        // Mock ProcessBuilder and Process to simulate non-zero exit code
        val mockProcess = mockk<Process>()
        val mockProcessBuilder = mockk<ProcessBuilder>()

        // Mock process output streams
        val errorOutput = "Error: No metadata found for ISBN 9780547928210"
        val errorStream = ByteArrayInputStream(errorOutput.toByteArray())
        val outputStream = ByteArrayInputStream("".toByteArray())

        every { mockProcess.inputStream } returns outputStream
        every { mockProcess.errorStream } returns errorStream
        every { mockProcess.waitFor(any<Long>(), any()) } returns false

        every { mockProcessBuilder.command(any<List<String>>()) } returns mockProcessBuilder
        every { mockProcessBuilder.redirectOutput(any<ProcessBuilder.Redirect>()) } returns
            mockProcessBuilder
        every { mockProcessBuilder.start() } returns mockProcess

        // Mock the factory instead of static ProcessBuilder
        every { processBuilderFactory.createProcessBuilder() } returns mockProcessBuilder

        val metadataRequest = MetadataRequestDto(isbn = "9780547928210")
        val config = mapOf<String, String>()

        val result = calibreMetadataProvider.fetchMetadata(metadataRequest, config)

        Assertions.assertTrue(result.isPresent)
        val metadataDto = result.get()
        Assertions.assertEquals(MetadataError.EXIT_CODE_NOT_ZERO, metadataDto.errorType)
        Assertions.assertEquals(errorOutput, metadataDto.pluginErrorMessage)
        Assertions.assertNull(metadataDto.title)
        Assertions.assertTrue(metadataDto.authors.isEmpty())
    }

    @Test
    fun testFetchMetadataWithExceptionCaught() {
        // Mock ProcessBuilder to throw exception
        val mockProcessBuilder = mockk<ProcessBuilder>()
        val exceptionMessage = "No such file or directory"

        every { mockProcessBuilder.command(any<List<String>>()) } returns mockProcessBuilder
        every { mockProcessBuilder.redirectOutput(any<ProcessBuilder.Redirect>()) } returns
            mockProcessBuilder
        every { mockProcessBuilder.start() } throws IOException(exceptionMessage)

        // Mock the factory instead of static ProcessBuilder
        every { processBuilderFactory.createProcessBuilder() } returns mockProcessBuilder

        val metadataRequest = MetadataRequestDto(isbn = "9780547928210")
        val config = mapOf<String, String>()

        val result = calibreMetadataProvider.fetchMetadata(metadataRequest, config)

        Assertions.assertTrue(result.isPresent)
        val metadataDto = result.get()
        Assertions.assertEquals(MetadataError.EXCEPTION_CAUGHT, metadataDto.errorType)
        Assertions.assertEquals(exceptionMessage, metadataDto.pluginErrorMessage)
        Assertions.assertNull(metadataDto.title)
        Assertions.assertTrue(metadataDto.authors.isEmpty())
    }

    @Test
    fun testFetchMetadataWithSuccessfulProcess() {
        // Mock ProcessBuilder and Process to simulate successful execution
        mockkStatic(ProcessBuilder::class)

        val mockProcess = mockk<Process>()
        val mockProcessBuilder = mockk<ProcessBuilder>()

        // Mock successful XML output
        val xmlOutput = createBasicXmlOutput()

        val outputStream = ByteArrayInputStream(xmlOutput.toByteArray())

        every { mockProcess.inputStream } returns outputStream
        every { mockProcess.waitFor(any<Long>(), any()) } returns true

        every { mockProcessBuilder.command(any<List<String>>()) } returns mockProcessBuilder
        every { mockProcessBuilder.redirectOutput(any<ProcessBuilder.Redirect>()) } returns
            mockProcessBuilder
        every { mockProcessBuilder.start() } returns mockProcess

        // Mock the factory instead of static ProcessBuilder
        every { processBuilderFactory.createProcessBuilder() } returns mockProcessBuilder

        val metadataRequest = MetadataRequestDto(isbn = "9780547928210")
        val config = mapOf<String, String>()

        val result = calibreMetadataProvider.fetchMetadata(metadataRequest, config)

        Assertions.assertTrue(result.isPresent)
        val metadataDto = result.get()
        Assertions.assertNull(metadataDto.errorType)
        Assertions.assertNull(metadataDto.pluginErrorMessage)
        Assertions.assertEquals("The Fellowship of the Ring", metadataDto.title)
        Assertions.assertEquals("9780547928210", metadataDto.isbn13)
        Assertions.assertEquals(1, metadataDto.authors.size)
        Assertions.assertEquals("J. R. R. Tolkien", metadataDto.authors.first())

        unmockkStatic(ProcessBuilder::class)
    }

    @Test
    fun testFetchMetadataWithASIN() {
        // Test ASIN handling logic
        mockkStatic(ProcessBuilder::class)

        val mockProcess = mockk<Process>()
        val mockProcessBuilder = mockk<ProcessBuilder>()

        // Mock successful XML output for ASIN
        val xmlOutput =
            createBasicXmlOutput(
                author = "J. R. R. Tolkien",
                identifierScheme = "AMAZON",
                identifier = "B007978NPG",
            )

        val outputStream = ByteArrayInputStream(xmlOutput.toByteArray())

        every { mockProcess.inputStream } returns outputStream
        every { mockProcess.waitFor(any<Long>(), any()) } returns true

        every { mockProcessBuilder.command(any<List<String>>()) } returns mockProcessBuilder
        every { mockProcessBuilder.redirectOutput(any<ProcessBuilder.Redirect>()) } returns
            mockProcessBuilder
        every { mockProcessBuilder.start() } returns mockProcess

        // Mock the factory instead of static ProcessBuilder
        every { processBuilderFactory.createProcessBuilder() } returns mockProcessBuilder

        val metadataRequest = MetadataRequestDto(isbn = "B007978NPG") // ASIN
        val config = mapOf<String, String>()

        val result = calibreMetadataProvider.fetchMetadata(metadataRequest, config)

        // Verify the result is present and contains expected metadata
        Assertions.assertTrue(result.isPresent)
        val metadataDto = result.get()
        Assertions.assertNull(metadataDto.errorType)
        Assertions.assertNull(metadataDto.pluginErrorMessage)
        Assertions.assertEquals("The Fellowship of the Ring", metadataDto.title)
        Assertions.assertEquals("B007978NPG", metadataDto.amazonId)
        Assertions.assertEquals(1, metadataDto.authors.size)
        Assertions.assertEquals("J. R. R. Tolkien", metadataDto.authors.first())

        unmockkStatic(ProcessBuilder::class)
    }

    @Test
    fun testFetchMetadataWithOnlyUseCorePlugins() {
        // Test onlyUseCorePlugins configuration
        mockkStatic(ProcessBuilder::class)

        val mockProcess = mockk<Process>()
        val mockProcessBuilder = mockk<ProcessBuilder>()

        // Mock successful XML output
        val xmlOutput = createBasicXmlOutput()

        val outputStream = ByteArrayInputStream(xmlOutput.toByteArray())

        every { mockProcess.inputStream } returns outputStream
        every { mockProcess.waitFor(any<Long>(), any()) } returns true

        every { mockProcessBuilder.command(any<List<String>>()) } returns mockProcessBuilder
        every { mockProcessBuilder.redirectOutput(any<ProcessBuilder.Redirect>()) } returns
            mockProcessBuilder
        every { mockProcessBuilder.start() } returns mockProcess

        // Mock the factory instead of static ProcessBuilder
        every { processBuilderFactory.createProcessBuilder() } returns mockProcessBuilder

        val metadataRequest = MetadataRequestDto(isbn = "9780547928210")
        val config = mapOf("onlyUseCorePlugins" to "true")

        val result = calibreMetadataProvider.fetchMetadata(metadataRequest, config)

        // Verify the result is present and contains expected metadata
        Assertions.assertTrue(result.isPresent)
        val metadataDto = result.get()
        Assertions.assertNull(metadataDto.errorType)
        Assertions.assertNull(metadataDto.pluginErrorMessage)
        Assertions.assertEquals("The Fellowship of the Ring", metadataDto.title)
        Assertions.assertEquals("9780547928210", metadataDto.isbn13)
        Assertions.assertEquals(1, metadataDto.authors.size)
        Assertions.assertEquals("J. R. R. Tolkien", metadataDto.authors.first())

        unmockkStatic(ProcessBuilder::class)
    }

    @Test
    fun testFetchMetadataWithFetchCoverDisabled() {
        // Test fetchCover configuration
        mockkStatic(ProcessBuilder::class)

        val mockProcess = mockk<Process>()
        val mockProcessBuilder = mockk<ProcessBuilder>()

        // Mock successful XML output
        val xmlOutput = createBasicXmlOutput()

        val outputStream = ByteArrayInputStream(xmlOutput.toByteArray())

        every { mockProcess.inputStream } returns outputStream
        every { mockProcess.waitFor(any<Long>(), any()) } returns true

        every { mockProcessBuilder.command(any<List<String>>()) } returns mockProcessBuilder
        every { mockProcessBuilder.redirectOutput(any<ProcessBuilder.Redirect>()) } returns
            mockProcessBuilder
        every { mockProcessBuilder.start() } returns mockProcess

        // Mock the factory instead of static ProcessBuilder
        every { processBuilderFactory.createProcessBuilder() } returns mockProcessBuilder

        val metadataRequest = MetadataRequestDto(isbn = "9780547928210")
        val config = mapOf("fetchCover" to "false")

        val result = calibreMetadataProvider.fetchMetadata(metadataRequest, config)

        // Verify the result is present and contains expected metadata
        Assertions.assertTrue(result.isPresent)
        val metadataDto = result.get()
        Assertions.assertNull(metadataDto.errorType)
        Assertions.assertNull(metadataDto.pluginErrorMessage)
        Assertions.assertEquals("The Fellowship of the Ring", metadataDto.title)
        Assertions.assertEquals("9780547928210", metadataDto.isbn13)
        Assertions.assertEquals(1, metadataDto.authors.size)
        Assertions.assertEquals("J. R. R. Tolkien", metadataDto.authors.first())
        // When fetchCover is disabled, the image field should be null or empty
        Assertions.assertNull(metadataDto.image)

        unmockkStatic(ProcessBuilder::class)
    }

    @Test
    fun testParseOpf() {
        val input =
            "<?xml version='1.0' encoding='utf-8'?>\n" +
                "<package xmlns=\"http://www.idpf.org/2007/opf\" unique-identifier=\"uuid_id\" version=\"2.0\">\n" +
                "    <metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:opf=\"http://www.idpf.org/2007/opf\">\n" +
                "        <dc:identifier opf:scheme=\"calibre\" id=\"calibre_id\">381c7cb8-1766-4914-8eb1-cb0a143e613c</dc:identifier>\n" +
                "        <dc:identifier opf:scheme=\"uuid\" id=\"uuid_id\">7ea8ba77-7623-47b1-b846-9e2b22f8fcd7</dc:identifier>\n" +
                "        <dc:title>Manesh: Les Sentiers des Astres, T1</dc:title>\n" +
                "        <dc:creator opf:file-as=\"Inconnu(e)\" opf:role=\"aut\">Stefan Platteau</dc:creator>\n" +
                "        <dc:contributor opf:file-as=\"calibre\" opf:role=\"bkp\">calibre (5.32.0) [https://calibre-ebook.com]" +
                "</dc:contributor>\n" +
                "        <dc:date>2014-04-03T11:14:34.623717+00:00</dc:date>\n" +
                "        <dc:description>Quelque part dans la nordique forêt du Vyanthryr, les gabarres du capitaine Rana " +
                "remontent le fleuve " +
                "vers les sources sacrées où réside le Roi-diseur, l’oracle dont lesavoir pourrait inverser le cours de la guerre civile." +
                "  À bord, " +
                "une poignée de guerriers prêts à tout pour sauver leur patrie. Mais qui, parmi eux, connaît vraiment le dessein du " +
                "capitaine ? " +
                "Même le Barde, son homme de confiance, n’a pas exploré tous les replis de son âme. Et lorsque les bateliers recueillent " +
                "un moribond " +
                "qui dérive au fil de l’eau, à des milles et des milles de toute civilisation, de nouvelles questions surgissent. " +
                "Qui est Le Bâtard ? " +
                "Que faisait-il dans la forêt ? Est-il un danger potentiel, ou au contraire le formidable allié qui pourrait sauver " +
                "l’expédition de " +
                "l’anéantissement pur et simple ?</dc:description>\n" +
                "        <dc:publisher>Les Moutons Électriques</dc:publisher>\n" +
                "        <dc:identifier opf:scheme=\"GOOGLE\">5vKyDwAAQBAJ</dc:identifier>\n" +
                "        <dc:identifier opf:scheme=\"ISBN\">9782361831523</dc:identifier>\n" +
                "        <dc:language>fra</dc:language>\n" +
                "        <dc:subject>Fiction</dc:subject>\n" +
                "        <dc:subject>Fantasy</dc:subject>\n" +
                "        <dc:subject>Historical</dc:subject>\n" +
                "        <meta name=\"calibre:author_link_map\" content=\"{}\"/>\n" +
                "    </metadata>\n" +
                "    <guide>\n" +
                "        <reference type=\"cover\" title=\"Couverture\" href=\"manesh.jpg\"/>\n" +
                "    </guide>\n" +
                "</package>"
        val metadata = opfParser.parseOpf(input)
        Assertions.assertEquals("5vKyDwAAQBAJ", metadata.googleId)
        Assertions.assertEquals("9782361831523", metadata.isbn13)
        Assertions.assertEquals(1, metadata.authors.size)
        Assertions.assertEquals(
            "Stefan Platteau",
            metadata.authors
                .stream()
                .findFirst()
                .get(),
        )
    }

    @Test
    fun testParseOpfSeveralAuthorsOnOneLine() {
        val input =
            "<?xml version='1.0' encoding='utf-8'?>\n" +
                "<package xmlns=\"http://www.idpf.org/2007/opf\" unique-identifier=\"uuid_id\" version=\"2.0\">\n" +
                "    <metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:opf=\"http://www.idpf.org/2007/opf\">\n" +
                "        <dc:identifier opf:scheme=\"calibre\" id=\"calibre_id\">381c7cb8-1766-4914-8eb1-cb0a143e613c</dc:identifier>\n" +
                "        <dc:identifier opf:scheme=\"uuid\" id=\"uuid_id\">7ea8ba77-7623-47b1-b846-9e2b22f8fcd7</dc:identifier>\n" +
                "        <dc:title>Manesh: Les Sentiers des Astres, T1</dc:title>\n" +
                "        <dc:creator opf:file-as=\"Inconnu(e)\" opf:role=\"aut\">Richard Corben; Dave Stewart ; Richard Corben; " +
                "</dc:creator>\n" +
                "        <dc:contributor opf:file-as=\"calibre\" opf:role=\"bkp\">calibre (5.32.0) [https://calibre-ebook.com]" +
                "</dc:contributor>\n" +
                "        <dc:date>2014-04-03T11:14:34.623717+00:00</dc:date>\n" +
                "        <dc:description>Quelque part dans la nordique forêt du Vyanthryr, les gabarres du capitaine Rana " +
                "remontent le fleuve vers les sources sacrées où réside le Roi-diseur, l’oracle dont lesavoir pourrait inverser " +
                "le cours de la guerre civile.  À bord, une poignée de guerriers prêts à tout pour sauver leur patrie. Mais qui, " +
                "parmi eux, connaît vraiment le dessein du capitaine ? Même le Barde, son homme de confiance, n’a pas exploré tous " +
                "les replis de son âme. Et lorsque les bateliers recueillent un moribond qui dérive au fil de l’eau, à des milles " +
                "et des milles de toute civilisation, de nouvelles questions surgissent. Qui est Le Bâtard ? Que faisait-il dans " +
                "la forêt ? Est-il un danger potentiel, ou au contraire le formidable allié qui pourrait sauver l’expédition de " +
                "l’anéantissement pur et simple ?</dc:description>\n" +
                "        <dc:publisher>Les Moutons Électriques</dc:publisher>\n" +
                "        <dc:identifier opf:scheme=\"GOOGLE\">5vKyDwAAQBAJ</dc:identifier>\n" +
                "        <dc:identifier opf:scheme=\"ISBN\">9782361831523</dc:identifier>\n" +
                "        <dc:language>fra</dc:language>\n" +
                "        <dc:subject>Fiction</dc:subject>\n" +
                "        <dc:subject>Fantasy</dc:subject>\n" +
                "        <dc:subject>Historical</dc:subject>\n" +
                "        <meta name=\"calibre:author_link_map\" content=\"{}\"/>\n" +
                "    </metadata>\n" +
                "    <guide>\n" +
                "        <reference type=\"cover\" title=\"Couverture\" href=\"manesh.jpg\"/>\n" +
                "    </guide>\n" +
                "</package>"
        val metadata = opfParser.parseOpf(input)
        Assertions.assertEquals("5vKyDwAAQBAJ", metadata.googleId)
        Assertions.assertEquals("9782361831523", metadata.isbn13)
        Assertions.assertEquals(2, metadata.authors.size)
    }

    @Test
    fun testParseOpfNoRole() {
        val input = File(this::class.java.getResource("/metadata/content.opf").file).readText(Charsets.UTF_8)
        val metadata = opfParser.parseOpf(input)
        Assertions.assertNull(metadata.googleId)
        Assertions.assertEquals("9782370491190", metadata.isbn13)
        Assertions.assertNull(metadata.isbn10)
        Assertions.assertEquals(1, metadata.authors.size)
        Assertions.assertEquals(1, metadata.tags.size)
    }

    @Test
    fun testParseOpfSeriesInBelongTo() {
        val input = File(this::class.java.getResource("/metadata/Panik-im-Paradies.opf").file).readText(Charsets.UTF_8)
        val metadata = opfParser.parseOpf(input)
        Assertions.assertNull(metadata.googleId)
        Assertions.assertEquals("9783440077894", metadata.isbn13)
        Assertions.assertEquals("222735", metadata.goodreadsId)
        Assertions.assertEquals("Panik im Paradies", metadata.title)
        Assertions.assertEquals("Kosmos", metadata.publisher)
        Assertions.assertEquals("Die drei ??? Kids", metadata.series)
        Assertions.assertEquals(1.0, metadata.numberInSeries)
        Assertions.assertNull(metadata.isbn10)
        Assertions.assertEquals(2, metadata.authors.size)
        Assertions.assertEquals(1, metadata.tags.size)
    }

    @Test
    fun testParseOpfSeriesInBelongToExternalIds() {
        val input = File(this::class.java.getResource("/metadata/Die-Drei-3.opf").file).readText(Charsets.UTF_8)
        val metadata = opfParser.parseOpf(input)
        Assertions.assertEquals("9783440077931", metadata.isbn13)
        Assertions.assertEquals("71348", metadata.goodreadsId)
        Assertions.assertEquals("3440077934", metadata.amazonId)
        Assertions.assertEquals("pPamZwEACAAJ", metadata.googleId)
        Assertions.assertEquals("Die Drei Fragezeichen-Kids, Bd.3, Invasion Der Fliegen", metadata.title)
        Assertions.assertEquals("Franckh-Kosmos Verlag", metadata.publisher)
        Assertions.assertEquals("Die drei ??? Kids", metadata.series)
        Assertions.assertEquals(3.0, metadata.numberInSeries)
        Assertions.assertNull(metadata.isbn10)
        Assertions.assertEquals(2, metadata.authors.size)
        Assertions.assertEquals(3, metadata.tags.size)
    }

    @Test
    fun testParseOpfExtraCharactersAroundXml() {
        var input =
            """
            |b'<?xml version=\'1.0\' encoding=\'utf-8\'?>\n<package xmlns="http://www.idpf.org/2007/opf" unique-identifier="uuid_id" version="2.0">\n    <metadata xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:opf="http://www.idpf.org/2007/opf">\n        <dc:identifier opf:scheme="calibre" id="calibre_id">daabc6db-820e-43d8-bf8c-2b89bcf07460</dc:identifier>\n        <dc:identifier opf:scheme="uuid" id="uuid_id">dd53ac75-d6c3-4428-ab42-f054a5358a36</dc:identifier>\n        <dc:title>The Fellowship of the Ring</dc:title>\n        <dc:creator opf:file-as="Unknown" opf:role="aut">J. R. R. Tolkien</dc:creator>\n        <dc:contributor opf:file-as="calibre" opf:role="bkp">calibre (4.99.4) [https://calibre-ebook.com]</dc:contributor>\n        <dc:date>1986-08-15T08:20:07.274919+00:00</dc:date>\n        <dc:description>One Ring to rule them all, One Ring to find them, One Ring to bring them all and in the darkeness bind them. In ancient times the Rings of Power were crafted by the Elven-smiths, and Sauron, The Dark Lord, forged the One Ring, filling it with his own power so that he could rule all others. But the One Ring was taken from him, and though he sought it throughout Middle-earth, it remained lost to him. After many ages it fell into the hands of Bilbo Baggins, as told in The Hobbit. In a sleepy village in the Shire, young Frodo Baggins finds himself faced with an immense task, as his elderly cousin Bilbo entrusts the Ring to his care. Frodo must leave his home and make a perilous journey across Middle-earth to the Cracks of Doom, there to destroy the Ring and foil the Dark Lord in his evil purpose.</dc:description>\n        <dc:publisher>Ballantine Books</dc:publisher>\n        <dc:identifier opf:scheme="GOOGLE">3flBjgEACAAJ</dc:identifier>\n        <dc:identifier opf:scheme="ISBN">9780808520764</dc:identifier>\n        <dc:identifier opf:scheme="AMAZON">0345339703</dc:identifier>\n        <dc:language>eng</dc:language>\n        <dc:subject>Fiction</dc:subject>\n        <dc:subject>Classics</dc:subject>\n        <dc:subject>Fantasy</dc:subject>\n        <dc:subject>Epic</dc:subject>\n        <dc:subject>Juvenile Fiction</dc:subject>\n        <dc:subject>Fantasy &amp; Magic</dc:subject>\n        <meta name="calibre:author_link_map" content="{}"/>\n        <meta name="calibre:rating" content="5"/>\n    </metadata>\n    <guide/>\n</package>\n'
            """.trimMargin()
        if (!input.startsWith('<')) {
            input = calibreMetadataProvider.cleanXml(input)
        }
        val metadata = opfParser.parseOpf(input)
        Assertions.assertEquals("3flBjgEACAAJ", metadata.googleId)
        Assertions.assertEquals("0345339703", metadata.amazonId)
        Assertions.assertNull(metadata.goodreadsId)
        Assertions.assertEquals("9780808520764", metadata.isbn13)
        Assertions.assertEquals("The Fellowship of the Ring", metadata.title)
        Assertions.assertEquals(1, metadata.authors.size)
        Assertions.assertEquals("J. R. R. Tolkien", metadata.authors.first())
    }

    @Test
    fun testASINMatch() {
        // https://www.amazon.com/Fellowship-Ring-Being-First-Rings-ebook/dp/B007978NPG -- Kindle Edition
        var input = "B007978NPG"
        val resultingCodeType = calibreMetadataProvider.determineCodeType(input)
        Assertions.assertEquals("ASIN", resultingCodeType)
    }

    @Test
    fun testISBN10Match() {
        // https://www.amazon.com/Fellowship-Ring-Being-First-Rings/dp/0547928211 -- Paperback ISBN 10
        var input = "0547928211"
        val resultingCodeType = calibreMetadataProvider.determineCodeType(input)
        Assertions.assertEquals("ISBN-10", resultingCodeType)
    }

    @Test
    fun testISB13match() {
        // // https://www.amazon.com/Fellowship-Ring-Being-First-Rings/dp/0547928211 -- Paperback ISBN 13
        var plainInput = "9780547928210"
        val plainResultingCodeType = calibreMetadataProvider.determineCodeType(plainInput)
        var dashedInput = "978-0547928210"
        val dashedResultingCodeType = calibreMetadataProvider.determineCodeType(dashedInput)
        Assertions.assertEquals("ISBN-13", plainResultingCodeType)
        Assertions.assertEquals("ISBN-13", dashedResultingCodeType)
    }

    @Test
    fun testMetadataErrorEnumValues() {
        // Test that MetadataError enum contains the expected values
        Assertions.assertEquals("EXIT_CODE_NOT_ZERO", MetadataError.EXIT_CODE_NOT_ZERO.name)
        Assertions.assertEquals("EXCEPTION_CAUGHT", MetadataError.EXCEPTION_CAUGHT.name)

        // Test that we can create MetadataDto with these error types
        val exitCodeDto = MetadataDto()
        exitCodeDto.errorType = MetadataError.EXIT_CODE_NOT_ZERO
        Assertions.assertEquals(MetadataError.EXIT_CODE_NOT_ZERO, exitCodeDto.errorType)

        val exceptionDto = MetadataDto()
        exceptionDto.errorType = MetadataError.EXCEPTION_CAUGHT
        Assertions.assertEquals(MetadataError.EXCEPTION_CAUGHT, exceptionDto.errorType)
    }

    @Test
    fun testMetadataDtoErrorFields() {
        // Test that MetadataDto can properly hold error information
        val dto = MetadataDto()

        // Test setting and getting error type
        dto.errorType = MetadataError.EXIT_CODE_NOT_ZERO
        Assertions.assertEquals(MetadataError.EXIT_CODE_NOT_ZERO, dto.errorType)

        // Test setting and getting plugin error message
        dto.pluginErrorMessage = "Test error message"
        Assertions.assertEquals("Test error message", dto.pluginErrorMessage)

        // Test that other fields remain null when error is set
        Assertions.assertNull(dto.title)
        Assertions.assertNull(dto.isbn13)
        Assertions.assertTrue(dto.authors.isEmpty())
    }

    @Test
    fun testMetadataDtoWithBothErrorAndData() {
        // Test that MetadataDto can hold both error information and partial data
        val dto = MetadataDto()

        // Set some metadata
        dto.title = "Test Book"
        dto.isbn13 = "9780547928210"
        dto.authors.add("Test Author")

        // Set error information
        dto.errorType = MetadataError.EXCEPTION_CAUGHT
        dto.pluginErrorMessage = "Partial data retrieved before error occurred"

        // Verify both data and error information are preserved
        Assertions.assertEquals("Test Book", dto.title)
        Assertions.assertEquals("9780547928210", dto.isbn13)
        Assertions.assertEquals(1, dto.authors.size)
        Assertions.assertEquals("Test Author", dto.authors.first())
        Assertions.assertEquals(MetadataError.EXCEPTION_CAUGHT, dto.errorType)
        Assertions.assertEquals("Partial data retrieved before error occurred", dto.pluginErrorMessage)
    }
}
