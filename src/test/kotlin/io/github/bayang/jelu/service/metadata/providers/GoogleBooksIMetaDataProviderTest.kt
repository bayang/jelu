package io.github.bayang.jelu.service.metadata.providers

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dto.MetadataDto
import io.github.bayang.jelu.dto.MetadataRequestDto
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class GoogleBooksIMetaDataProviderTest {

    @Test
    fun fetchMetadata_fromCorrectIsbn_returnsBookMetaDataDto() {
        // Given
        val webClient = WebClient.builder()
            .exchangeFunction { clientRequest: ClientRequest? ->
                Assertions.assertEquals("q=isbn:9781785650406&key=fake-google-api-key", clientRequest?.url()?.query)
                Mono.just(
                    ClientResponse.create(HttpStatus.OK)
                        .header("content-type", "application/json")
                        .body(
                            "{\n" +
                                "  \"kind\": \"books#volumes\",\n" +
                                "  \"totalItems\": 1,\n" +
                                "  \"items\": [\n" +
                                "    {\n" +
                                "      \"kind\": \"books#volume\",\n" +
                                "      \"id\": \"0GJOMQAACAAJ\",\n" +
                                "      \"etag\": \"Cizh+H/q+S4\",\n" +
                                "      \"selfLink\": \"https://www.googleapis.com/books/v1/volumes/0GJOMQAACAAJ\",\n" +
                                "      \"volumeInfo\": {\n" +
                                "        \"title\": \"Genesis Fleet - Vanguard\",\n" +
                                "        \"authors\": [\n" +
                                "          \"Jack Campbell\"\n" +
                                "        ],\n" +
                                "        \"publishedDate\": \"2017-05\",\n" +
                                "        \"industryIdentifiers\": [\n" +
                                "          {\n" +
                                "            \"type\": \"ISBN_10\",\n" +
                                "            \"identifier\": \"1785650408\"\n" +
                                "          },\n" +
                                "          {\n" +
                                "            \"type\": \"ISBN_13\",\n" +
                                "            \"identifier\": \"9781785650406\"\n" +
                                "          }\n" +
                                "        ],\n" +
                                "        \"readingModes\": {\n" +
                                "          \"text\": false,\n" +
                                "          \"image\": false\n" +
                                "        },\n" +
                                "        \"printType\": \"BOOK\",\n" +
                                "        \"maturityRating\": \"NOT_MATURE\",\n" +
                                "        \"allowAnonLogging\": false,\n" +
                                "        \"contentVersion\": \"preview-1.0.0\",\n" +
                                "        \"panelizationSummary\": {\n" +
                                "          \"containsEpubBubbles\": false,\n" +
                                "          \"containsImageBubbles\": false\n" +
                                "        },\n" +
                                "        \"imageLinks\": {\n" +
                                "          \"smallThumbnail\": \"http://books.google.com/books/content?id=0GJOMQAACAAJ&printsec=frontcover&img=1&zoom=5&source=gbs_api\",\n" +
                                "          \"thumbnail\": \"http://books.google.com/books/content?id=0GJOMQAACAAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api\"\n" +
                                "        },\n" +
                                "        \"language\": \"en\",\n" +
                                "        \"previewLink\": \"http://books.google.fr/books?id=0GJOMQAACAAJ&dq=isbn:9781785650406&hl=&cd=1&source=gbs_api\",\n" +
                                "        \"infoLink\": \"http://books.google.fr/books?id=0GJOMQAACAAJ&dq=isbn:9781785650406&hl=&source=gbs_api\",\n" +
                                "        \"canonicalVolumeLink\": \"https://books.google.com/books/about/Genesis_Fleet_Vanguard.html?hl=&id=0GJOMQAACAAJ\"\n" +
                                "      },\n" +
                                "      \"saleInfo\": {\n" +
                                "        \"country\": \"FR\",\n" +
                                "        \"saleability\": \"NOT_FOR_SALE\",\n" +
                                "        \"isEbook\": false\n" +
                                "      },\n" +
                                "      \"accessInfo\": {\n" +
                                "        \"country\": \"FR\",\n" +
                                "        \"viewability\": \"NO_PAGES\",\n" +
                                "        \"embeddable\": false,\n" +
                                "        \"publicDomain\": false,\n" +
                                "        \"textToSpeechPermission\": \"ALLOWED\",\n" +
                                "        \"epub\": {\n" +
                                "          \"isAvailable\": false\n" +
                                "        },\n" +
                                "        \"pdf\": {\n" +
                                "          \"isAvailable\": false\n" +
                                "        },\n" +
                                "        \"webReaderLink\": \"http://play.google.com/books/reader?id=0GJOMQAACAAJ&hl=&printsec=frontcover&source=gbs_api\",\n" +
                                "        \"accessViewStatus\": \"NONE\",\n" +
                                "        \"quoteSharingAllowed\": false\n" +
                                "      },\n" +
                                "      \"searchInfo\": {\n" +
                                "        \"textSnippet\": \"&quot;Earth is no longer the center of the universe.\"\n" +
                                "      }\n" +
                                "    }\n" +
                                "  ]\n" +
                                "}\n"
                        )
                        .build()
                )
            }.build()

        val jeluProperties = JeluProperties(
            JeluProperties.Database(""),
            JeluProperties.Files("", "", true),
            JeluProperties.Session(1),
            JeluProperties.Cors(),
            JeluProperties.Metadata(JeluProperties.Calibre("")),
            JeluProperties.Auth(JeluProperties.Ldap(), JeluProperties.Proxy()),
            listOf(JeluProperties.MetaDataProvider("google", true, "fake-google-api-key"))
        )
        val service = GoogleBooksIMetaDataProvider(webClient, jeluProperties)

        // When
        val result: MetadataDto = service.fetchMetadata(
            MetadataRequestDto("9781785650406"),
            mapOf()
        )?.block()!!

        // Then
        Assertions.assertNotNull(result)
        Assertions.assertEquals("Genesis Fleet - Vanguard", result.title)
        Assertions.assertEquals("0GJOMQAACAAJ", result.googleId)
        Assertions.assertEquals("1785650408", result.isbn10)
        Assertions.assertEquals("9781785650406", result.isbn13)
        Assertions.assertEquals(mutableSetOf("Jack Campbell"), result.authors)
        Assertions.assertEquals(
            "http://books.google.com/books/content?id=0GJOMQAACAAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api",
            result.image
        )
        Assertions.assertEquals("en", result.language)
        Assertions.assertEquals("2017-05", result.publishedDate)
        Assertions.assertEquals("&quot;Earth is no longer the center of the universe.", result.summary)
    }
}
