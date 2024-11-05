package io.github.bayang.jelu.service.metadata.providers

/**
 * see https://www.wikidata.org/wiki/Property:P212
 */
class Wikidata {
    companion object {
        const val PREFIX = "wdt:"
        const val TITLE = "${PREFIX}P1476"
        const val ISBN13 = "${PREFIX}P212"
        const val ISBN10 = "${PREFIX}P957"
        const val EDITION_OR_TRANSLATION = "${PREFIX}P629"
        const val PUBLISHER = "${PREFIX}P123"
        const val PUBLICATION_DATE = "${PREFIX}P577"
        const val NB_PAGES = "${PREFIX}P1104"
        const val GOODREADS_ID = "${PREFIX}P2969"
        const val LANGUAGE_OF_WORK_OR_NAME = "${PREFIX}P407"
        const val AUTHOR = "${PREFIX}P50"
        const val GENRE = "${PREFIX}P136"
        const val SERIES = "${PREFIX}P179"
        const val OPEN_LIBRARY_ID = "${PREFIX}P648"
        const val MAIN_SUBJECT = "${PREFIX}P921"
        const val LIBRARYTHING_WORK_ID = "${PREFIX}P1085"
        const val ISFDB_TITLE_ID = "${PREFIX}P1274"
        const val GOODREADS_WORK_ID = "${PREFIX}P8383"
        const val NOOSFERE_BOOK_ID = "${PREFIX}P5571"
        const val SERIES_ORDINAL = "${PREFIX}P1545"
    }
}
