package io.github.bayang.jelu.search

import mu.KotlinLogging
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexUpgrader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.queryparser.classic.ParseException
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.TermQuery
import org.apache.lucene.store.Directory
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class LuceneHelper(
    private val directory: Directory,
    private val indexAnalyzer: Analyzer,
    private val searchAnalyzer: Analyzer,
) {

    private fun getIndexWriterConfig() = IndexWriterConfig(indexAnalyzer)

    fun getIndexWriter() = IndexWriter(directory, getIndexWriterConfig())

    fun getIndexReader(): DirectoryReader = DirectoryReader.open(directory)

    fun indexExists(): Boolean = DirectoryReader.indexExists(directory)

    fun setIndexVersion(version: Int) {
        getIndexWriter().use { indexWriter ->
            val doc = Document().apply {
                add(StringField("index_version", version.toString(), Field.Store.YES))
                add(StringField("type", "index_version", Field.Store.NO))
            }
            indexWriter.updateDocument(Term("type", "index_version"), doc)
        }
        logger.info { "Lucene index version: ${getIndexVersion()}" }
    }

    fun getIndexVersion(): Int =
        getIndexReader().use { index ->
            val searcher = IndexSearcher(index)
            val topDocs = searcher.search(TermQuery(Term("type", "index_version")), 1)
            topDocs.scoreDocs.map { searcher.storedFields().document(it.doc)["index_version"] }.firstOrNull()?.toIntOrNull() ?: 1
        }

    fun searchEntitiesIds(searchTerm: String?, entity: LuceneEntity): List<String>? {
        return if (!searchTerm.isNullOrBlank()) {
            try {
                val fieldsQuery = MultiFieldQueryParser(entity.defaultFields, searchAnalyzer).apply {
                    defaultOperator = QueryParser.Operator.AND
                }.parse("$searchTerm *:*")

                val typeQuery = TermQuery(Term(LuceneEntity.TYPE, entity.type))

                val booleanQuery = BooleanQuery.Builder()
                    .add(fieldsQuery, BooleanClause.Occur.MUST)
                    .add(typeQuery, BooleanClause.Occur.MUST)
                    .build()

                getIndexReader().use { index ->
                    val searcher = IndexSearcher(index)
                    val topDocs = searcher.search(booleanQuery, index.numDocs())
                    topDocs.scoreDocs.map { searcher.storedFields().document(it.doc)[entity.id] }
                }
            } catch (e: ParseException) {
                emptyList()
            } catch (e: Exception) {
                logger.error(e) { "Error fetching entities from index" }
                emptyList()
            }
        } else {
            null
        }
    }
    fun searchEntitiesIdsForTags(searchTerms: List<String>, entity: LuceneEntity): List<String>? {
        return if (searchTerms.isNotEmpty()) {
            try {
                val terms = mutableListOf<Term>()
                for (term in searchTerms) {
                    terms.add(Term("tag_ex", term))
                }
                val typeQuery = TermQuery(Term(LuceneEntity.TYPE, entity.type))

                val booleanIntermQuery = BooleanQuery.Builder()
                    .add(typeQuery, BooleanClause.Occur.MUST)

                for (t in terms) {
                    booleanIntermQuery.add(TermQuery(t), BooleanClause.Occur.MUST)
                }
                val booleanQuery = booleanIntermQuery.build()
                getIndexReader().use { index ->
                    val searcher = IndexSearcher(index)
                    val topDocs = searcher.search(booleanQuery, index.numDocs())
                    topDocs.scoreDocs.map { searcher.storedFields().document(it.doc)[entity.id] }
                }
            } catch (e: ParseException) {
                emptyList()
            } catch (e: Exception) {
                logger.error(e) { "Error fetching entities from index" }
                emptyList()
            }
        } else {
            null
        }
    }

    fun upgradeIndex() {
        IndexUpgrader(directory, getIndexWriterConfig(), true).upgrade()
        logger.info { "Lucene index upgraded" }
    }
}
