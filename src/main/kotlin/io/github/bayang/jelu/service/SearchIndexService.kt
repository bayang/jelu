package io.github.bayang.jelu.service

import io.github.bayang.jelu.dao.Book
import io.github.bayang.jelu.dao.BookRepository
import io.github.bayang.jelu.search.LuceneEntity
import io.github.bayang.jelu.search.LuceneHelper
import io.github.bayang.jelu.search.toDocument
import mu.KotlinLogging
import org.apache.lucene.document.Document
import org.apache.lucene.index.Term
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.math.ceil
import kotlin.time.measureTime

const val INDEX_VERSION = 3
private val logger = KotlinLogging.logger {}

@Component
class SearchIndexService(
    private val luceneHelper: LuceneHelper,
    private val bookRepository: BookRepository,
) {

    fun upgradeIndex() {
        luceneHelper.upgradeIndex()
        luceneHelper.setIndexVersion(INDEX_VERSION)
    }

    @Transactional
    fun rebuildIndex(entities: Set<LuceneEntity>? = null) {
        val targetEntities = entities ?: LuceneEntity.values().toSet()
        logger.info { "Rebuild index for: ${targetEntities.map { it.type }}" }
        targetEntities.forEach {
            when (it) {
                LuceneEntity.Book -> rebuildIndex(
                    it,
                    {
                            p: Pageable ->
                        bookRepository.findAllNoFilters(null, null, null, null, null, null, null, null, p)
                    },
                    { e: Book -> e.toDocument() },
                )
                LuceneEntity.Author -> logger.debug { "no authors index yet" }
            }
        }
        luceneHelper.setIndexVersion(INDEX_VERSION)
    }

    private fun <T> rebuildIndex(entity: LuceneEntity, provider: (Pageable) -> Page<out T>, toDoc: (T) -> Document) {
        logger.info { "Rebuilding index for ${entity.name}" }

        val count = provider(Pageable.ofSize(1)).totalElements
        val batchSize = 5_000
        val pages = ceil(count.toDouble() / batchSize).toInt()
        logger.info { "Number of entities: $count and pages : $pages" }

        luceneHelper.getIndexWriter().use { indexWriter ->
            measureTime {
                indexWriter.deleteDocuments(Term(LuceneEntity.TYPE, entity.type))

                (0 until pages).forEach { page ->
                    logger.info { "Processing page ${page + 1} of $pages ($batchSize elements)" }
                    val entityDocs = provider(PageRequest.of(page, batchSize)).content
                        .map { toDoc(it) }
                    indexWriter.addDocuments(entityDocs)
                }
            }.also { duration ->
                logger.info { "Wrote ${entity.name} index in $duration" }
            }
        }
    }

    fun bookAdded(book: Book) {
        addEntity(book.toDocument())
    }

    fun bookUpdated(book: Book) {
        updateEntity(LuceneEntity.Book, book.id.value.toString(), book.toDocument())
    }

    fun bookUpdated(bookId: UUID) {
        bookUpdated(bookRepository.findBookById(bookId))
    }

    fun booksUpdated(bookIds: List<UUID>) {
        bookIds.forEach { bookUpdated(bookRepository.findBookById(it)) }
    }

    fun bookDeleted(bookId: UUID) {
        deleteEntity(LuceneEntity.Book, bookId.toString())
    }

    fun authorUpdated(authorId: UUID) {
        var books: Page<Book>
        do {
            books = bookRepository.findAuthorBooksByIdNoFilters(authorId, Pageable.ofSize(30))
            books.forEach { deleteEntity(LuceneEntity.Book, it.id.value.toString()) }
            books.forEach { addEntity(it.toDocument()) }
        }
        while (books.hasNext())
    }

    fun seriesUpdated(seriesId: UUID) {
        var books: Page<Book>
        do {
            books = bookRepository.findSeriesBooksByIdNoFilters(seriesId, Pageable.ofSize(30))
            books.forEach { deleteEntity(LuceneEntity.Book, it.id.value.toString()) }
            books.forEach { addEntity(it.toDocument()) }
        }
        while (books.hasNext())
    }

    private fun addEntity(doc: Document) {
        luceneHelper.getIndexWriter().use { indexWriter ->
            indexWriter.addDocument(doc)
        }
    }

    private fun updateEntity(entity: LuceneEntity, entityId: String, newDoc: Document) {
        luceneHelper.getIndexWriter().use { indexWriter ->
            indexWriter.updateDocument(Term(entity.id, entityId), newDoc)
        }
    }

    private fun deleteEntity(entity: LuceneEntity, entityId: String) {
        luceneHelper.getIndexWriter().use { indexWriter ->
            indexWriter.deleteDocuments(Term(entity.id, entityId))
        }
    }
}
