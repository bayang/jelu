package io.github.bayang.jelu.service.metadata.providers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dto.MetadataDto
import io.github.bayang.jelu.dto.MetadataRequestDto
import jakarta.annotation.Resource
import mu.KotlinLogging
import org.springframework.boot.info.BuildProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.util.Optional

private val logger = KotlinLogging.logger {}

const val USER_AGENT = "jelu/"

@Service
class InventaireIoMetadataProvider(
    @Resource(name = "springRestClient") private val restClient: RestClient,
    private val properties: JeluProperties,
    private val objectMapper: ObjectMapper,
    private val buildProperties: BuildProperties,
) : IMetaDataProvider {

    private val _name = "inventaireio"

    private val inventaireHost = "https://inventaire.io"
    private val inventaireApi = "$inventaireHost/api/"

    private val defaultLanguageCode: String = "en"

    override fun fetchMetadata(metadataRequestDto: MetadataRequestDto, config: Map<String, String>): Optional<MetadataDto> {
        if (!metadataRequestDto.isbn.isNullOrBlank()) {
            val isbn = metadataRequestDto.isbn.replace("-", "", true)
            return restClient.get()
                .uri(inventaireApi) {
                        uriBuilder ->
                    uriBuilder
                        .path("entities")
                        .queryParam("action", "by-uris")
                        .queryParam("uris", "isbn:$isbn")
                        .build()
                }
                .header(HttpHeaders.USER_AGENT, USER_AGENT + buildProperties.version)
                .exchange { clientRequest, clientResponse ->
                    if (clientResponse.statusCode == HttpStatus.OK) {
                        val bodyString = clientResponse.bodyTo(String::class.java)
                        val node = objectMapper.readTree(bodyString).get("entities")
                        var p = parseIsbnResult(node, isbn)
                        p = enrichWithEditionResult(p)
                        p = enrichWithAuhors(p)
                        p = enrichWithGenres(p)
                        p = enrichWithSeries(p)
                        Optional.of(toMetadataDto(p))
                    } else {
                        val b = clientResponse.bodyTo(String::class.java).orEmpty()
                        logger.error { "error fetching metadata from inventaire.io : ${clientResponse.statusCode}; $b " }
                        Optional.empty()
                    }
                }
        } else if (!metadataRequestDto.title.isNullOrBlank()) {
            var p = searchByTitle(metadataRequestDto.title)
            p = enrichWithEditionResult(p)
            p = enrichWithAuhors(p)
            p = enrichWithSeries(p)
            p = enrichWithGenres(p)
            return Optional.of(toMetadataDto(p))
        } else if (!metadataRequestDto.authors.isNullOrBlank()) {
            return searchAuthorsTitles(metadataRequestDto.authors)
        }
        return Optional.empty()
    }

    private fun searchAuthorsTitles(author: String): Optional<MetadataDto> {
        return restClient.get()
            .uri(inventaireApi) {
                    uriBuilder ->
                uriBuilder
                    .path("search")
                    .queryParam("types", "humans")
                    .queryParam("search", author)
                    .build()
            }
            .header(HttpHeaders.USER_AGENT, USER_AGENT + buildProperties.version)
            .exchange { clientRequest, clientResponse ->
                if (clientResponse.statusCode == HttpStatus.OK) {
                    val bodyString = clientResponse.bodyTo(String::class.java)
                    val node = objectMapper.readTree(bodyString).get("results")
                    val authorUri = parseSearchAuthorsResults(node)
                    var p = getAuthorBooks(authorUri)
                    p = enrichWithEditionResult(p)
                    p = enrichWithAuhors(p)
                    p = enrichWithSeries(p)
                    p = enrichWithGenres(p)
                    Optional.of(toMetadataDto(p))
                } else {
                    logger.error { "error fetching metadata from inventaire.io : ${clientResponse.statusCode} " }
                    Optional.empty()
                }
            }
    }

    private fun searchByTitle(title: String): ParsingDto {
        return restClient.get()
            .uri(inventaireApi) {
                    uriBuilder ->
                uriBuilder
                    .path("search")
                    .queryParam("types", "works")
                    .queryParam("search", title)
                    .build()
            }
            .header(HttpHeaders.USER_AGENT, USER_AGENT + buildProperties.version)
            .exchange { clientRequest, clientResponse ->
                if (clientResponse.statusCode == HttpStatus.OK) {
                    val bodyString = clientResponse.bodyTo(String::class.java)
                    val node = objectMapper.readTree(bodyString).get("results")
                    parseSearchResults(node)
                } else {
                    logger.error { "error fetching metadata from inventaire.io : ${clientResponse.statusCode} " }
                    ParsingDto(MetadataDto(), "")
                }
            }
    }

    private fun parseSearchResults(node: JsonNode): ParsingDto {
        val firstResult = node.asIterable().first()
        val dto = MetadataDto()
        if (firstResult.has("label")) {
            dto.title = firstResult.get("label").asText()
        }
        if (firstResult.has("description")) {
            dto.summary = firstResult.get("description").asText()
        }
        val res = ParsingDto(dto, "")
        if (firstResult.has("uri")) {
            res.editionClaim = firstResult.get("uri").asText()
        }
        if (firstResult.has("image")) {
            val imgParent = firstResult.get("image")
            if (!imgParent.isEmpty) {
                val img = imgParent[0].asText()
                dto.image = imagePath(img)
            }
        }
        return res
    }

    private fun parseSearchAuthorsResults(node: JsonNode): String {
        val firstResult = node.asIterable().first()
        if (firstResult.has("uri")) {
            return firstResult.get("uri").asText()
        }
        return ""
    }

    private fun parseIsbnResult(node: JsonNode, isbn: String): ParsingDto {
        val dto = MetadataDto()
        val parsingDto = ParsingDto(dto, "")
        if (node.has("isbn:$isbn")) {
            val data = node.get("isbn:$isbn")
            if (data.has("claims")) {
                val claims = data["claims"]
                parseClaims(claims, parsingDto)
            }
            if (data.has("originalLang")) {
                dto.language = data["originalLang"].asText()
            }
            if (data.has("image") && data.get("image").get("url") != null) {
                dto.image = imagePath(data["image"]["url"].asText())
            }
            if (data.has("invId")) {
                dto.inventaireId = data["invId"].asText()
            }
        }
        return parsingDto
    }

    /**
     * a local image path
     * (ex: /img/entities/57883743aa7c6ad25885a63e6e94349ec4f71562)
     * that you are can then request resized
     * (ex: https://inventaire.io/img/entities/300x300/57883743aa7c6ad25885a63e6e94349ec4f71562)
     * a Wikimedia Commons file name
     * (ex: Les Deux Nigauds Comtesse de Ségur.jpg) which can then also be requested resized
     * (ex: https://commons.wikimedia.org/wiki/Special:FilePath/Les%20Deux%20Nigauds%20Comtesse%20de%20S%C3%A9gur.jpg?width=300,
     * be sure to URL-encode the filename)
     */
    fun imagePath(url: String): String {
        return if (url.startsWith("/img/")) {
            inventaireHost + url
        } else {
            "https://commons.wikimedia.org/wiki/Special:FilePath/$url"
        }
    }

    fun parseClaims(node: JsonNode, dto: ParsingDto) {
        parseClaims(node, dto.metadataDto)
        if (node.has(Wikidata.AUTHOR)) {
            val authors = node[Wikidata.AUTHOR].asIterable()
            authors.forEach { dto.authorsClaims.add(it.asText()) }
        }
        if (dto.editionClaim.isBlank() && node.has(Wikidata.EDITION_OR_TRANSLATION)) {
            dto.editionClaim = getFieldOrNull(Wikidata.EDITION_OR_TRANSLATION, node).orEmpty()
        }
        if (node.has(Wikidata.SERIES)) {
            val series = node[Wikidata.SERIES].asIterable()
            series.forEach { dto.seriesClaims.add(it.asText()) }
        }
        if (node.has(Wikidata.GENRE)) {
            val genres = node[Wikidata.GENRE].asIterable()
            genres.forEach { dto.genresClaims.add(it.asText()) }
        }
    }

    private fun parseClaims(node: JsonNode, dto: MetadataDto) {
        if (dto.title == null) {
            dto.title = getFieldOrNull(Wikidata.TITLE, node)
        }
        if (dto.isbn10 == null) {
            dto.isbn10 = getFieldOrNull(Wikidata.ISBN10, node)
        }
        if (dto.isbn13 == null) {
            dto.isbn13 = getFieldOrNull(Wikidata.ISBN13, node)
        }
        if (dto.pageCount == null && node.has(Wikidata.NB_PAGES)) {
            dto.pageCount = node[Wikidata.NB_PAGES][0].asInt()
        }
        if (dto.goodreadsId == null) {
            dto.goodreadsId = getFieldOrNull(Wikidata.GOODREADS_ID, node)
        }
        if (dto.publishedDate == null) {
            dto.publishedDate = getFieldOrNull(Wikidata.PUBLICATION_DATE, node)
        }
        if (dto.librarythingId == null) {
            dto.librarythingId = getFieldOrNull(Wikidata.LIBRARYTHING_WORK_ID, node)
        }
        if (dto.isfdbId == null) {
            dto.isfdbId = getFieldOrNull(Wikidata.ISFDB_TITLE_ID, node)
        }
        if (dto.openlibraryId == null) {
            dto.openlibraryId = getFieldOrNull(Wikidata.OPEN_LIBRARY_ID, node)
        }
        if (dto.noosfereId == null) {
            dto.noosfereId = getFieldOrNull(Wikidata.NOOSFERE_BOOK_ID, node)
        }
        if (dto.numberInSeries == null && node.has(Wikidata.SERIES_ORDINAL)) {
            dto.numberInSeries = node[Wikidata.SERIES_ORDINAL][0].asDouble()
        }
    }

    private fun getFieldOrNull(fieldName: String, node: JsonNode): String? {
        if (node.has(fieldName)) {
            return node[fieldName][0].asText()
        }
        return null
    }

    private fun getAuthorBooks(authorUri: String): ParsingDto {
        if (authorUri.isNotBlank()) {
            return restClient.get()
                .uri(inventaireApi) {
                        uriBuilder ->
                    uriBuilder
                        .path("entities")
                        .queryParam("action", "author-works")
                        .queryParam("uri", authorUri)
                        .build()
                }
                .header(HttpHeaders.USER_AGENT, USER_AGENT + buildProperties.version)
                .exchange { clientRequest, clientResponse ->
                    if (clientResponse.statusCode == HttpStatus.OK) {
                        val bodyString = clientResponse.bodyTo(String::class.java)
                        val node = objectMapper.readTree(bodyString).get("works")
                        parseAuthorWorks(node)
                    } else {
                        logger.error { "error fetching metadata from edition from inventaire.io : ${clientResponse.statusCode} " }
                        ParsingDto(MetadataDto(), "")
                    }
                }
        }
        return ParsingDto(MetadataDto(), "")
    }

    private fun parseAuthorWorks(node: JsonNode): ParsingDto {
        val parsingDto = ParsingDto(MetadataDto(), "")
        val first = node.asIterable().first()
        if (first.has("uri")) {
            parsingDto.editionClaim = first["uri"].asText()
        }
        if (first.has("serie")) {
            parsingDto.seriesClaims.add(first["serie"].asText())
        }
        return parsingDto
    }

    private fun enrichWithEditionResult(dto: ParsingDto): ParsingDto {
        if (dto.editionClaim.isNotBlank()) {
            return restClient.get()
                .uri(inventaireApi) {
                        uriBuilder ->
                    uriBuilder
                        .path("entities")
                        .queryParam("action", "by-uris")
                        .queryParam("uris", dto.editionClaim)
                        .build()
                }
                .header(HttpHeaders.USER_AGENT, USER_AGENT + buildProperties.version)
                .exchange { clientRequest, clientResponse ->
                    if (clientResponse.statusCode == HttpStatus.OK) {
                        val bodyString = clientResponse.bodyTo(String::class.java)
                        val node = objectMapper.readTree(bodyString).get("entities")
                        parseEditionResult(node, dto)
                    } else {
                        logger.error { "error fetching metadata from edition from inventaire.io : ${clientResponse.statusCode} " }
                        dto
                    }
                }
        }
        return dto
    }

    fun enrichWithAuhors(dto: ParsingDto): ParsingDto {
        if (dto.authorsClaims.isNotEmpty()) {
            dto.authorsClaims.stream()
                .forEach { author ->
                    val res = fetchDataPage(author)
                    dto.metadataDto.authors.add(res)
                }
        }
        return dto
    }

    fun enrichWithSeries(dto: ParsingDto): ParsingDto {
        if (dto.seriesClaims.isNotEmpty()) {
            dto.seriesClaims.stream()
                .forEach { series ->
                    val res = fetchDataPage(series)
                    dto.metadataDto.series = res
                }
        }
        return dto
    }

    fun enrichWithGenres(dto: ParsingDto): ParsingDto {
        if (dto.genresClaims.isNotEmpty()) {
            dto.genresClaims.stream()
                .forEach { genre ->
                    val res = fetchDataPage(genre)
                    dto.metadataDto.tags.add(res)
                }
        }
        return dto
    }

    fun fetchDataPage(dataId: String): String {
        return restClient.get()
            .uri(inventaireApi) {
                    uriBuilder ->
                uriBuilder
                    .path("entities")
                    .queryParam("action", "by-uris")
                    .queryParam("uris", dataId)
                    .build()
            }
            .header(HttpHeaders.USER_AGENT, USER_AGENT + buildProperties.version)
            .exchange { clientRequest, clientResponse ->
                if (clientResponse.statusCode == HttpStatus.OK) {
                    val bodyString = clientResponse.bodyTo(String::class.java)
                    val node = objectMapper.readTree(bodyString).get("entities")
                    parseDataBody(node, dataId)
                } else {
                    logger.error { "error fetching metadata from data : $dataId from inventaire.io : ${clientResponse.statusCode} " }
                    ""
                }
            }
    }

    /**
     * author page, genre page, series page
     */
    fun parseDataBody(node: JsonNode, dataId: String): String {
        var res = ""
        if (node.has(dataId)) {
            val data = node.get(dataId)
            if (data.has("labels")) {
                val dataLabels = data.get("labels")
                if (!getPreferredLanguage().isNullOrBlank() && dataLabels.has(getPreferredLanguage())) {
                    res = dataLabels.get(getPreferredLanguage()).asText()
                } else if (dataLabels.has(defaultLanguageCode)) {
                    res = dataLabels.get(defaultLanguageCode).asText()
                } else if (dataLabels.size() > 0) {
                    res = dataLabels.first().asText()
                }
            }
        }
        return res
    }

    private fun parseEditionResult(node: JsonNode, dto: ParsingDto): ParsingDto {
        if (node.has(dto.editionClaim)) {
            val data = node.get(dto.editionClaim)
            if (data.has("descriptions")) {
                val descs = data.get("descriptions")
                if (!getPreferredLanguage().isNullOrBlank() && descs.has(getPreferredLanguage())) {
                    dto.metadataDto.summary = descs.get(getPreferredLanguage()).asText()
                } else if (descs.has(defaultLanguageCode)) {
                    dto.metadataDto.summary = descs.get(defaultLanguageCode).asText()
                } else {
                    dto.metadataDto.summary = descs.first().asText()
                }
            }
            if (data.has("claims")) {
                val claims = data.get("claims")
                parseClaims(claims, dto)
            }
            if (dto.metadataDto.title == null) {
                dto.metadataDto.title = parseDataBody(node, dto.editionClaim)
            }
            if (data.has("invId") && dto.metadataDto.inventaireId == null) {
                dto.metadataDto.inventaireId = data["invId"].asText()
            }
        }
        return dto
    }

    private fun toMetadataDto(dto: ParsingDto): MetadataDto {
        return dto.metadataDto
    }

    override fun name(): String {
        return _name
    }

    private fun getPreferredLanguage(): String? = properties
        .metadataProviders
        ?.find { it.isEnabled && it.name == _name }
        ?.config
}

data class ParsingDto(
    val metadataDto: MetadataDto,
    var editionClaim: String,
    val authorsClaims: MutableSet<String> = mutableSetOf(),
    val seriesClaims: MutableSet<String> = mutableSetOf(),
    val genresClaims: MutableSet<String> = mutableSetOf(),
)
