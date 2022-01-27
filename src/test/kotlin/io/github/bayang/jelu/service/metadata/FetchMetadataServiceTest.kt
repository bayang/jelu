package io.github.bayang.jelu.service.metadata

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class FetchMetadataServiceTest(@Autowired private val fetchMetadataService: FetchMetadataService) {

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
        val metadata = fetchMetadataService.parseOpf(input)
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
        val metadata = fetchMetadataService.parseOpf(input)
        Assertions.assertEquals("5vKyDwAAQBAJ", metadata.googleId)
        Assertions.assertEquals("9782361831523", metadata.isbn13)
        Assertions.assertEquals(2, metadata.authors.size)
    }
}
