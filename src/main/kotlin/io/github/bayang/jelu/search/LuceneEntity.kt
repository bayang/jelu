package io.github.bayang.jelu.search

import io.github.bayang.jelu.dao.Book
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField

enum class LuceneEntity(
    val type: String,
    val id: String,
    val defaultFields: Array<String>,
) {
    Book("book", "book_id", arrayOf("title", "isbn")),
    Author("author", "author_id", arrayOf("name")),
    ;

    companion object {
        const val TYPE = "type"
    }
}

fun Book.toDocument() =
    Document().apply {
        add(TextField("title", title, Field.Store.NO))
        if (!originalTitle.isNullOrBlank()) add(TextField("original_title", originalTitle, Field.Store.NO))
        if (!isbn10.isNullOrBlank()) add(TextField("isbn", isbn10, Field.Store.NO))
        if (!isbn13.isNullOrBlank()) add(TextField("isbn", isbn13, Field.Store.NO))
        tags.forEach {
            add(TextField("tag", it.name, Field.Store.NO))
            // add tag name as a string field as well so that it is stored with its original casing
            // and is not tokenized. It is used for custom lists since we want to search by exact list of tags
            add(StringField("tag_ex", it.name, Field.Store.NO))
            add(StringField("tag_id", it.id.value.toString(), Field.Store.YES))
        }
        authors.forEach {
            add(TextField("author", it.name, Field.Store.NO))
        }
        translators.forEach {
            add(TextField("translator", it.name, Field.Store.NO))
        }
        narrators.forEach {
            add(TextField("narrator", it.name, Field.Store.NO))
        }
        if (!seriesBak.isNullOrBlank()) add(TextField("series", seriesBak, Field.Store.NO))
        seriesAndOrder.forEach {
            add(TextField("series", it.series.name, Field.Store.NO))
        }
        if (!language.isNullOrBlank()) add(TextField("language", language, Field.Store.NO))
        if (!publishedDate.isNullOrBlank()) add(TextField("published_date", publishedDate, Field.Store.NO))
        if (!publisher.isNullOrBlank()) add(TextField("publisher", publisher, Field.Store.NO))
        if (!summary.isNullOrBlank()) add(TextField("summary", summary, Field.Store.NO))
        if (!googleId.isNullOrBlank()) add(TextField("googleId", googleId, Field.Store.NO))
        if (!goodreadsId.isNullOrBlank()) add(TextField("goodreadsId", goodreadsId, Field.Store.NO))
        if (!amazonId.isNullOrBlank()) add(TextField("amazonId", amazonId, Field.Store.NO))
        if (!librarythingId.isNullOrBlank()) add(TextField("librarythingId", librarythingId, Field.Store.NO))
        if (!noosfereId.isNullOrBlank()) add(TextField("noosfereId", noosfereId, Field.Store.NO))
        if (!isfdbId.isNullOrBlank()) add(TextField("isfdbId", isfdbId, Field.Store.NO))
        if (!inventaireId.isNullOrBlank()) add(TextField("inventaireId", inventaireId, Field.Store.NO))
        if (!openlibraryId.isNullOrBlank()) add(TextField("openlibraryId", openlibraryId, Field.Store.NO))

        add(StringField(LuceneEntity.TYPE, LuceneEntity.Book.type, Field.Store.NO))
        add(StringField(LuceneEntity.Book.id, id.value.toString(), Field.Store.YES))
    }
