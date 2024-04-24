package io.github.bayang.jelu.service.metadata.providers

import io.github.bayang.jelu.service.metadata.OpfParser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File

@SpringBootTest
class CalibreMetadataProviderTest(
    @Autowired private val calibreMetadataProvider: CalibreMetadataProvider,
    @Autowired private val opfParser: OpfParser,
) {

    @Test
    fun testParseOpf() {
        val input = "<?xml version='1.0' encoding='utf-8'?>\n" +
            "<package xmlns=\"http://www.idpf.org/2007/opf\" unique-identifier=\"uuid_id\" version=\"2.0\">\n" +
            "    <metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:opf=\"http://www.idpf.org/2007/opf\">\n" +
            "        <dc:identifier opf:scheme=\"calibre\" id=\"calibre_id\">381c7cb8-1766-4914-8eb1-cb0a143e613c</dc:identifier>\n" +
            "        <dc:identifier opf:scheme=\"uuid\" id=\"uuid_id\">7ea8ba77-7623-47b1-b846-9e2b22f8fcd7</dc:identifier>\n" +
            "        <dc:title>Manesh: Les Sentiers des Astres, T1</dc:title>\n" +
            "        <dc:creator opf:file-as=\"Inconnu(e)\" opf:role=\"aut\">Stefan Platteau</dc:creator>\n" +
            "        <dc:contributor opf:file-as=\"calibre\" opf:role=\"bkp\">calibre (5.32.0) [https://calibre-ebook.com]</dc:contributor>\n" +
            "        <dc:date>2014-04-03T11:14:34.623717+00:00</dc:date>\n" +
            "        <dc:description>Quelque part dans la nordique forêt du Vyanthryr, les gabarres du capitaine Rana remontent le fleuve vers les sources sacrées où réside le Roi-diseur, l’oracle dont lesavoir pourrait inverser le cours de la guerre civile.  À bord, une poignée de guerriers prêts à tout pour sauver leur patrie. Mais qui, parmi eux, connaît vraiment le dessein du capitaine ? Même le Barde, son homme de confiance, n’a pas exploré tous les replis de son âme. Et lorsque les bateliers recueillent un moribond qui dérive au fil de l’eau, à des milles et des milles de toute civilisation, de nouvelles questions surgissent. Qui est Le Bâtard ? Que faisait-il dans la forêt ? Est-il un danger potentiel, ou au contraire le formidable allié qui pourrait sauver l’expédition de l’anéantissement pur et simple ?</dc:description>\n" +
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
        Assertions.assertEquals("Stefan Platteau", metadata.authors.stream().findFirst().get())
    }

    @Test
    fun testParseOpfSeveralAuthorsOnOneLine() {
        val input = "<?xml version='1.0' encoding='utf-8'?>\n" +
            "<package xmlns=\"http://www.idpf.org/2007/opf\" unique-identifier=\"uuid_id\" version=\"2.0\">\n" +
            "    <metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:opf=\"http://www.idpf.org/2007/opf\">\n" +
            "        <dc:identifier opf:scheme=\"calibre\" id=\"calibre_id\">381c7cb8-1766-4914-8eb1-cb0a143e613c</dc:identifier>\n" +
            "        <dc:identifier opf:scheme=\"uuid\" id=\"uuid_id\">7ea8ba77-7623-47b1-b846-9e2b22f8fcd7</dc:identifier>\n" +
            "        <dc:title>Manesh: Les Sentiers des Astres, T1</dc:title>\n" +
            "        <dc:creator opf:file-as=\"Inconnu(e)\" opf:role=\"aut\">Richard Corben; Dave Stewart ; Richard Corben; </dc:creator>\n" +
            "        <dc:contributor opf:file-as=\"calibre\" opf:role=\"bkp\">calibre (5.32.0) [https://calibre-ebook.com]</dc:contributor>\n" +
            "        <dc:date>2014-04-03T11:14:34.623717+00:00</dc:date>\n" +
            "        <dc:description>Quelque part dans la nordique forêt du Vyanthryr, les gabarres du capitaine Rana remontent le fleuve vers les sources sacrées où réside le Roi-diseur, l’oracle dont lesavoir pourrait inverser le cours de la guerre civile.  À bord, une poignée de guerriers prêts à tout pour sauver leur patrie. Mais qui, parmi eux, connaît vraiment le dessein du capitaine ? Même le Barde, son homme de confiance, n’a pas exploré tous les replis de son âme. Et lorsque les bateliers recueillent un moribond qui dérive au fil de l’eau, à des milles et des milles de toute civilisation, de nouvelles questions surgissent. Qui est Le Bâtard ? Que faisait-il dans la forêt ? Est-il un danger potentiel, ou au contraire le formidable allié qui pourrait sauver l’expédition de l’anéantissement pur et simple ?</dc:description>\n" +
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
        var input = """
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
}
