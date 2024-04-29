package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.AuthorDto
import io.github.bayang.jelu.dto.AuthorUpdateDto
import io.github.bayang.jelu.dto.BookCreateDto
import io.github.bayang.jelu.dto.BookUpdateDto
import io.github.bayang.jelu.dto.CreateReadingEventDto
import io.github.bayang.jelu.dto.CreateUserBookDto
import io.github.bayang.jelu.dto.LibraryFilter
import io.github.bayang.jelu.dto.Role
import io.github.bayang.jelu.dto.SeriesOrderDto
import io.github.bayang.jelu.dto.SeriesUpdateDto
import io.github.bayang.jelu.dto.TagDto
import io.github.bayang.jelu.dto.UserBookBulkUpdateDto
import io.github.bayang.jelu.dto.UserBookUpdateDto
import io.github.bayang.jelu.dto.fromBookCreateDto
import io.github.bayang.jelu.service.FileManager
import io.github.bayang.jelu.utils.nowInstant
import io.github.bayang.jelu.utils.sanitizeHtml
import mu.KotlinLogging
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.ExpressionAlias
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.Random
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.avg
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.orWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

fun parseSorts(sort: Sort, defaultSort: Pair<Expression<*>, SortOrder>, columns: List<Expression<*>>): Array<Pair<Expression<*>, SortOrder>> {
    val orders = mutableListOf<Pair<Expression<*>, SortOrder>>()
    // will fail for common names in multiple tables like creationDate, so
    // put most important table first
    // eg if sort by creationDate should be applied on BookTable rather than UserBookTable put it first
    for (o in sort) {
        // val found = columns.find { column -> column.name.replace("_", "", true).equals(o.property, true) }
        val found = columns.find { column ->
            when (column) {
                is Column -> column.name.replace("_", "", true).equals(o.property, true)
                is ExpressionAlias -> column.alias.replace("_", "", true).equals(o.property, true)
                else -> false
            }
        }
        if (found != null) {
            orders.add(Pair(found, if (o.isAscending) SortOrder.ASC_NULLS_LAST else SortOrder.DESC_NULLS_LAST))
        }
    }
    if (orders.isEmpty()) {
        orders.add(defaultSort)
    }
    return orders.toTypedArray()
}

fun parseSorts(sort: Sort, defaultSort: Pair<Expression<*>, SortOrder>, vararg tables: Table): Array<Pair<Expression<*>, SortOrder>> {
    val columns = mutableListOf<Column<*>>()
    for (table in tables) {
        columns.addAll(table.columns)
    }
    return parseSorts(sort, defaultSort, columns)
}

fun formatLike(input: String): String {
    return "%$input%"
}

// Convenience checker for whether sort is "random" vs column name
fun checkIfRandomSorting(pageable: Pageable): Boolean {
    // Check if sorting is specified and includes 'random'
    return pageable.sort.stream()
        .anyMatch { order -> order.property.equals("random", ignoreCase = true) }
}

@Repository
class BookRepository(
    val readingEventRepository: ReadingEventRepository,
    val fileManager: FileManager,
) {

    fun findAll(bookIds: List<String>?, pageable: Pageable, user: User, filter: LibraryFilter = LibraryFilter.ANY): Page<Book> {
        val booksWithSameIdAndUserHasUserbook = BookTable.join(UserBookTable, JoinType.LEFT)
            .slice(BookTable.id)
            .select { UserBookTable.book eq BookTable.id and (UserBookTable.user eq user.id) }
            .withDistinct()
        // required to avoir ambiguous column name "author.id" in joins below
        val translatorsAlias = AuthorTable.alias("trn")
        val query = BookTable.join(UserBookTable, JoinType.LEFT, onColumn = UserBookTable.book, otherColumn = BookTable.id)
            .join(BookAuthors, JoinType.LEFT, onColumn = BookTable.id, otherColumn = BookAuthors.book)
            .join(AuthorTable, JoinType.LEFT, onColumn = AuthorTable.id, otherColumn = BookAuthors.author)
            .join(BookTranslators, JoinType.LEFT, onColumn = BookTable.id, otherColumn = BookTranslators.book)
            .join(translatorsAlias, JoinType.LEFT, onColumn = translatorsAlias[AuthorTable.id], otherColumn = BookTranslators.translator)
            .join(BookTags, JoinType.LEFT, onColumn = BookTable.id, otherColumn = BookTags.book)
            .join(TagTable, JoinType.LEFT, onColumn = TagTable.id, otherColumn = BookTags.tag)
            .join(BookSeries, JoinType.LEFT, onColumn = BookTable.id, otherColumn = BookSeries.book)
            .join(SeriesTable, JoinType.LEFT, onColumn = SeriesTable.id, otherColumn = BookSeries.series)
            .slice(BookTable.columns)
            .selectAll()
            .withDistinct()
        if (!bookIds.isNullOrEmpty()) {
            val uuids = bookIds.map { UUID.fromString(it) }
            query.andWhere { BookTable.id inList uuids }
        }
        if (filter == LibraryFilter.ONLY_USER_BOOKS) {
            // only books where user has an userbook
            query.andWhere { UserBookTable.user eq user.id }
        } else if (filter == LibraryFilter.ONLY_NON_USER_BOOKS) {
            // only books where there are no userbooks or only other users have userbooks
            query.andWhere { BookTable.id notInSubQuery booksWithSameIdAndUserHasUserbook }
        }

        val total = query.count()
        if (checkIfRandomSorting(pageable)) {
            query.limit(pageable.pageSize)
            query.orderBy(Random())
        } else {
            query.limit(pageable.pageSize, pageable.offset)
            val orders: Array<Pair<Expression<*>, SortOrder>> = parseSorts(pageable.sort, Pair(BookTable.title, SortOrder.ASC_NULLS_LAST), BookTable)
            query.orderBy(*orders)
        }
        return PageImpl(
            query.map { resultRow -> wrapRow(resultRow, user.id.value) },
            pageable,
            total,
        )
    }

    fun findAllNoFilters(title: String?, isbn10: String?, isbn13: String?, series: String?, authors: List<String>?, translators: List<String>?, tags: List<String>?, pageable: Pageable): Page<Book> {
        // required to avoir ambiguous column name "author.id" in joins below
        val translatorsAlias = AuthorTable.alias("trn")
        val query = BookTable.join(UserBookTable, JoinType.LEFT, onColumn = UserBookTable.book, otherColumn = BookTable.id)
            .join(BookAuthors, JoinType.LEFT, onColumn = BookTable.id, otherColumn = BookAuthors.book)
            .join(AuthorTable, JoinType.LEFT, onColumn = AuthorTable.id, otherColumn = BookAuthors.author)
            .join(BookTranslators, JoinType.LEFT, onColumn = BookTable.id, otherColumn = BookTranslators.book)
            .join(translatorsAlias, JoinType.LEFT, onColumn = translatorsAlias[AuthorTable.id], otherColumn = BookTranslators.translator)
            .join(BookTags, JoinType.LEFT, onColumn = BookTable.id, otherColumn = BookTags.book)
            .join(TagTable, JoinType.LEFT, onColumn = TagTable.id, otherColumn = BookTags.tag)
            .join(BookSeries, JoinType.LEFT, onColumn = BookTable.id, otherColumn = BookSeries.book)
            .join(SeriesTable, JoinType.LEFT, onColumn = SeriesTable.id, otherColumn = BookSeries.series)
            .slice(BookTable.columns)
            .selectAll()
            .withDistinct()

        if (!title?.trim().isNullOrBlank()) {
            query.andWhere { BookTable.title like "%$title%" }
        }
        if (!isbn10?.trim().isNullOrBlank()) {
            query.andWhere { BookTable.isbn10 eq isbn10 }
        }
        if (!isbn13?.trim().isNullOrBlank()) {
            query.andWhere { BookTable.isbn13 eq isbn13 }
        }
        if (!series?.trim().isNullOrBlank()) {
            query.andWhere { SeriesTable.name like "%$series%" }
        }
        if (!authors.isNullOrEmpty()) {
            var first = true
            authors.forEach { author: String ->
                if (first) {
                    first = false
                    query.andWhere { AuthorTable.name like(formatLike(author)) }
                } else {
                    query.orWhere { AuthorTable.name like(formatLike(author)) }
                }
            }
        }
        if (!translators.isNullOrEmpty()) {
            var first = true
            translators.forEach { translator: String ->
                if (first) {
                    first = false
                    query.andWhere { translatorsAlias[AuthorTable.name] like(formatLike(translator)) }
                } else {
                    query.orWhere { translatorsAlias[AuthorTable.name] like(formatLike(translator)) }
                }
            }
        }
        if (!tags.isNullOrEmpty()) {
            var first = true
            tags.forEach { tag: String ->
                if (first) {
                    first = false
                    query.andWhere { TagTable.name like (formatLike(tag)) }
                } else {
                    query.orWhere { TagTable.name like (formatLike(tag)) }
                }
            }
        }
        val total = query.count()
        query.limit(pageable.pageSize, pageable.offset)
        val orders: Array<Pair<Expression<*>, SortOrder>> = parseSorts(pageable.sort, Pair(BookTable.title, SortOrder.ASC_NULLS_LAST), BookTable)
        query.orderBy(*orders)
        return PageImpl(
            query.map { resultRow -> wrapRow(resultRow) },
            pageable,
            total,
        )
    }

    fun findAll(title: String?, isbn10: String?, isbn13: String?, series: String?, authors: List<String>?, translators: List<String>?, tags: List<String>?, pageable: Pageable, user: User, filter: LibraryFilter = LibraryFilter.ANY): Page<Book> {
        val booksWithSameIdAndUserHasUserbook = BookTable.join(UserBookTable, JoinType.LEFT)
            .slice(BookTable.id)
            .select { UserBookTable.book eq BookTable.id and (UserBookTable.user eq user.id) }
            .withDistinct()
        // required to avoir ambiguous column name "author.id" in joins below
        val translatorsAlias = AuthorTable.alias("trn")
        val query = BookTable.join(UserBookTable, JoinType.LEFT, onColumn = UserBookTable.book, otherColumn = BookTable.id)
            .join(BookAuthors, JoinType.LEFT, onColumn = BookTable.id, otherColumn = BookAuthors.book)
            .join(AuthorTable, JoinType.LEFT, onColumn = AuthorTable.id, otherColumn = BookAuthors.author)
            .join(BookTranslators, JoinType.LEFT, onColumn = BookTable.id, otherColumn = BookTranslators.book)
            .join(translatorsAlias, JoinType.LEFT, onColumn = translatorsAlias[AuthorTable.id], otherColumn = BookTranslators.translator)
            .join(BookTags, JoinType.LEFT, onColumn = BookTable.id, otherColumn = BookTags.book)
            .join(TagTable, JoinType.LEFT, onColumn = TagTable.id, otherColumn = BookTags.tag)
            .join(BookSeries, JoinType.LEFT, onColumn = BookTable.id, otherColumn = BookSeries.book)
            .join(SeriesTable, JoinType.LEFT, onColumn = SeriesTable.id, otherColumn = BookSeries.series)
            .slice(BookTable.columns)
            .selectAll()
            .withDistinct()

        if (!title?.trim().isNullOrBlank()) {
            query.andWhere { BookTable.title like "%$title%" }
        }
        if (!isbn10?.trim().isNullOrBlank()) {
            query.andWhere { BookTable.isbn10 eq isbn10 }
        }
        if (!isbn13?.trim().isNullOrBlank()) {
            query.andWhere { BookTable.isbn13 eq isbn13 }
        }
        if (!series?.trim().isNullOrBlank()) {
            query.andWhere { SeriesTable.name like "%$series%" }
        }
        if (!authors.isNullOrEmpty()) {
            var first = true
            authors.forEach { author: String ->
                if (first) {
                    first = false
                    query.andWhere { AuthorTable.name like(formatLike(author)) }
                } else {
                    query.orWhere { AuthorTable.name like(formatLike(author)) }
                }
            }
        }
        if (!translators.isNullOrEmpty()) {
            var first = true
            translators.forEach { translator: String ->
                if (first) {
                    first = false
                    query.andWhere { translatorsAlias[AuthorTable.name] like(formatLike(translator)) }
                } else {
                    query.orWhere { translatorsAlias[AuthorTable.name] like(formatLike(translator)) }
                }
            }
        }
        if (!tags.isNullOrEmpty()) {
            var first = true
            tags.forEach { tag: String ->
                if (first) {
                    first = false
                    query.andWhere { TagTable.name like (formatLike(tag)) }
                } else {
                    query.orWhere { TagTable.name like (formatLike(tag)) }
                }
            }
        }
        if (filter == LibraryFilter.ONLY_USER_BOOKS) {
            // only books where user has an userbook
            query.andWhere { UserBookTable.user eq user.id }
        } else if (filter == LibraryFilter.ONLY_NON_USER_BOOKS) {
            // only books where there are no userbooks or only other users have userbooks
            query.andWhere { BookTable.id notInSubQuery booksWithSameIdAndUserHasUserbook }
        }

        val total = query.count()
        query.limit(pageable.pageSize, pageable.offset)
        val orders: Array<Pair<Expression<*>, SortOrder>> = parseSorts(pageable.sort, Pair(BookTable.title, SortOrder.ASC_NULLS_LAST), BookTable)
        query.orderBy(*orders)
        return PageImpl(
            query.map { resultRow -> wrapRow(resultRow, user.id.value) },
            pageable,
            total,
        )
    }

    fun findAllAuthors(name: String?, pageable: Pageable): Page<Author> {
        val query: Query = AuthorTable.selectAll()
        name?.let {
            query.andWhere { AuthorTable.name like "%$name%" }
        }
        val total = query.count()
        query.limit(pageable.pageSize, pageable.offset)
        val orders: Array<Pair<Expression<*>, SortOrder>> = parseSorts(pageable.sort, Pair(AuthorTable.name, SortOrder.ASC_NULLS_LAST), AuthorTable)
        query.orderBy(*orders)
        return PageImpl(
            query.map { resultRow -> Author.wrapRow(resultRow) },
            pageable,
            total,
        )
    }

    fun findAllTags(name: String?, pageable: Pageable): Page<Tag> {
        val query: Query = TagTable.selectAll()
        name?.let {
            query.andWhere { TagTable.name like "%$name%" }
        }
        val total = query.count()
        query.limit(pageable.pageSize, pageable.offset)
        val orders: Array<Pair<Expression<*>, SortOrder>> = parseSorts(pageable.sort, Pair(TagTable.name, SortOrder.ASC_NULLS_LAST), TagTable)
        query.orderBy(*orders)
        return PageImpl(
            query.map { resultRow -> Tag.wrapRow(resultRow) },
            pageable,
            total,
        )
    }

    fun findAllSeries(name: String?, pageable: Pageable): Page<Series> {
        val query: Query = SeriesTable.selectAll()
        name?.let {
            query.andWhere { SeriesTable.name like "%$name%" }
        }
        val total = query.count()
        query.limit(pageable.pageSize, pageable.offset)
        val orders: Array<Pair<Expression<*>, SortOrder>> = parseSorts(pageable.sort, Pair(SeriesTable.name, SortOrder.ASC_NULLS_LAST), SeriesTable)
        query.orderBy(*orders)
        return PageImpl(
            query.map { resultRow -> Series.wrapRow(resultRow) },
            pageable,
            total,
        )
    }

    fun findAuthorsByName(name: String): List<Author> {
        return Author.find { AuthorTable.name like "%$name%" }.toList()
    }

    fun findTagsByName(name: String): List<Tag> {
        return Tag.find { TagTable.name.lowerCase() eq name.lowercase() }.toList()
    }

    fun findSeriesByName(name: String): List<Series> {
        return Series.find { SeriesTable.name.lowerCase() eq name.lowercase() }.toList()
    }

    fun findBookById(bookId: UUID): Book = Book[bookId]

    fun findAuthorsById(authorId: UUID): Author = Author[authorId]

    fun findTagById(tagId: UUID): Tag = Tag[tagId]

    fun findSeriesById(seriesId: UUID): Series = Series[seriesId]

    fun findTagBooksById(
        tagId: UUID,
        user: User,
        pageable: Pageable,
        filter: LibraryFilter = LibraryFilter.ANY,
        eventTypes: List<ReadingEventType>?,
    ): Page<Book> {
        val booksWithSameIdAndUserHasUserbook = BookTable.join(UserBookTable, JoinType.LEFT)
            .slice(BookTable.id)
            .select { UserBookTable.book eq BookTable.id and (UserBookTable.user eq user.id) }
            .withDistinct()
        val query = BookTable.join(BookTags, JoinType.LEFT)
            .join(UserBookTable, JoinType.LEFT, onColumn = UserBookTable.book, otherColumn = BookTable.id)
            .slice(BookTable.columns)
            .select { BookTags.tag eq tagId }
        if (filter == LibraryFilter.ONLY_USER_BOOKS) {
            // only books where user has an userbook
            query.andWhere { UserBookTable.user eq user.id }
        } else if (filter == LibraryFilter.ONLY_NON_USER_BOOKS && eventTypes.isNullOrEmpty()) {
            // only books where there are no userbooks or only other users have userbooks
            query.andWhere { BookTable.id notInSubQuery booksWithSameIdAndUserHasUserbook }
        }
        if (!eventTypes.isNullOrEmpty()) {
            query.andWhere { UserBookTable.user eq user.id and(UserBookTable.lastReadingEvent inList eventTypes) }
        }
        query.withDistinct(true)
        val total = query.count()
        if (checkIfRandomSorting(pageable)) {
            query.limit(pageable.pageSize)
            query.orderBy(Random())
        } else {
            query.limit(pageable.pageSize, pageable.offset)
            val orders: Array<Pair<Expression<*>, SortOrder>> = parseSorts(pageable.sort, Pair(BookTable.title, SortOrder.ASC_NULLS_LAST), BookTable)
            query.orderBy(*orders)
        }
        return PageImpl(
            query.map { resultRow -> wrapRow(resultRow, user.id.value) },
            pageable,
            total,
        )
    }

    fun findSeriesBooksById(seriesId: UUID, user: User, pageable: Pageable, filter: LibraryFilter = LibraryFilter.ANY): Page<Book> {
        val booksWithSameIdAndUserHasUserbook = BookTable.join(UserBookTable, JoinType.LEFT)
            .slice(BookTable.id)
            .select { UserBookTable.book eq BookTable.id and (UserBookTable.user eq user.id) }
            .withDistinct()
        val query = BookTable.join(BookSeries, JoinType.LEFT)
            .join(UserBookTable, JoinType.LEFT, onColumn = UserBookTable.book, otherColumn = BookTable.id)
            .slice(BookTable.columns)
            .select { BookSeries.series eq seriesId }
        if (filter == LibraryFilter.ONLY_USER_BOOKS) {
            // only books where user has an userbook
            query.andWhere { UserBookTable.user eq user.id }
        } else if (filter == LibraryFilter.ONLY_NON_USER_BOOKS) {
            // only books where there are no userbooks or only other users have userbooks
            query.andWhere { BookTable.id notInSubQuery booksWithSameIdAndUserHasUserbook }
        }
        query.withDistinct(true)
        val total = query.count()
        query.limit(pageable.pageSize, pageable.offset)
        val orders: Array<Pair<Expression<*>, SortOrder>> = parseSorts(pageable.sort, Pair(BookTable.title, SortOrder.ASC_NULLS_LAST), BookTable)
        query.orderBy(*orders)
        return PageImpl(
            query.map { resultRow -> wrapRow(resultRow, user.id.value) },
            pageable,
            total,
        )
    }

    fun findTagBooksByIdNoFilters(tagId: UUID, pageable: Pageable): Page<Book> {
        val query = BookTable.join(BookTags, JoinType.LEFT)
            .join(UserBookTable, JoinType.LEFT, onColumn = UserBookTable.book, otherColumn = BookTable.id)
            .slice(BookTable.columns)
            .select { BookTags.tag eq tagId }

        query.withDistinct(true)
        val total = query.count()
        query.limit(pageable.pageSize, pageable.offset)
        val orders: Array<Pair<Expression<*>, SortOrder>> = parseSorts(pageable.sort, Pair(BookTable.title, SortOrder.ASC_NULLS_LAST), BookTable)
        query.orderBy(*orders)
        return PageImpl(
            query.map { resultRow -> wrapRow(resultRow) },
            pageable,
            total,
        )
    }

    fun findOrphanTags(pageable: Pageable): Page<Tag> {
        val query = TagTable.join(BookTags, JoinType.LEFT)
            .selectAll()
            .groupBy(TagTable.name)
            .having { BookTags.tag.count() eq(0) }

        query.withDistinct(true)
        val total = query.count()
        query.limit(pageable.pageSize, pageable.offset)
        val orders: Array<Pair<Expression<*>, SortOrder>> = parseSorts(pageable.sort, Pair(TagTable.name, SortOrder.ASC_NULLS_LAST), TagTable)
        query.orderBy(*orders)
        return PageImpl(
            query.map { resultRow -> wrapTagRow(resultRow) },
            pageable,
            total,
        )
    }

    fun findAuthorBooksById(authorId: UUID, user: User, pageable: Pageable, libaryFilter: LibraryFilter = LibraryFilter.ANY, role: Role = Role.ANY): Page<Book> {
        logger.trace { "role $role" }
        val booksWithSameIdAndUserHasUserbook = BookTable.join(UserBookTable, JoinType.LEFT)
            .slice(BookTable.id)
            .select { UserBookTable.book eq BookTable.id and (UserBookTable.user eq user.id) }
            .withDistinct()
        val query = BookTable.join(BookAuthors, JoinType.LEFT)
            .join(BookTranslators, JoinType.LEFT, onColumn = BookTranslators.book, otherColumn = BookTable.id)
            .join(UserBookTable, JoinType.LEFT, onColumn = UserBookTable.book, otherColumn = BookTable.id)
            .slice(BookTable.columns)
            .select {
                when (role) {
                    Role.ANY -> BookAuthors.author eq authorId or(BookTranslators.translator eq authorId)
                    Role.AUTHOR -> BookAuthors.author eq authorId
                    Role.TRANSLATOR -> BookTranslators.translator eq authorId
                }
            }
        if (libaryFilter == LibraryFilter.ONLY_USER_BOOKS) {
            // only books where user has an userbook
            query.andWhere { UserBookTable.user eq user.id }
        } else if (libaryFilter == LibraryFilter.ONLY_NON_USER_BOOKS) {
            // only books where there are no userbooks or only other users have userbooks
            query.andWhere { BookTable.id notInSubQuery booksWithSameIdAndUserHasUserbook }
        }
        query.withDistinct(true)
        val total = query.count()
        query.limit(pageable.pageSize, pageable.offset)
        val orders: Array<Pair<Expression<*>, SortOrder>> = parseSorts(pageable.sort, Pair(BookTable.title, SortOrder.ASC_NULLS_LAST), BookTable)
        query.orderBy(*orders)
        return PageImpl(
            query.map { resultRow -> wrapRow(resultRow, user.id.value) },
            pageable,
            total,
        )
    }

    fun findAuthorBooksByIdNoFilters(authorId: UUID, pageable: Pageable, role: Role = Role.ANY): Page<Book> {
        logger.trace { "role $role" }
        val query = BookTable.join(BookAuthors, JoinType.LEFT)
            .join(BookTranslators, JoinType.LEFT, onColumn = BookTranslators.book, otherColumn = BookTable.id)
            .join(UserBookTable, JoinType.LEFT, onColumn = UserBookTable.book, otherColumn = BookTable.id)
            .slice(BookTable.columns)
            .select {
                when (role) {
                    Role.ANY -> BookAuthors.author eq authorId or(BookTranslators.translator eq authorId)
                    Role.AUTHOR -> BookAuthors.author eq authorId
                    Role.TRANSLATOR -> BookTranslators.translator eq authorId
                }
            }
        query.withDistinct(true)
        val total = query.count()
        query.limit(pageable.pageSize, pageable.offset)
        val orders: Array<Pair<Expression<*>, SortOrder>> = parseSorts(pageable.sort, Pair(BookTable.title, SortOrder.ASC_NULLS_LAST), BookTable)
        query.orderBy(*orders)
        return PageImpl(
            query.map { resultRow -> wrapRow(resultRow) },
            pageable,
            total,
        )
    }

    fun findSeriesBooksByIdNoFilters(seriesId: UUID, pageable: Pageable): Page<Book> {
        val query = BookTable.join(BookSeries, JoinType.LEFT)
            .join(UserBookTable, JoinType.LEFT, onColumn = UserBookTable.book, otherColumn = BookTable.id)
            .slice(BookTable.columns)
            .select {
                BookSeries.series eq seriesId
            }
        query.withDistinct(true)
        val total = query.count()
        query.limit(pageable.pageSize, pageable.offset)
        val orders: Array<Pair<Expression<*>, SortOrder>> = parseSorts(pageable.sort, Pair(BookTable.title, SortOrder.ASC_NULLS_LAST), BookTable)
        query.orderBy(*orders)
        return PageImpl(
            query.map { resultRow -> wrapRow(resultRow) },
            pageable,
            total,
        )
    }

    fun wrapRow(resultRow: ResultRow, userId: UUID): Book {
        val e = Book.wrapRow(resultRow)
        val userbook = e.userBooks.firstOrNull { u -> u.user.id.value == userId }
        if (userbook != null) {
            e.userBookId = userbook.id.value
            e.userBook = userbook
        }
        return e
    }

    fun wrapRow(resultRow: ResultRow): Book {
        return Book.wrapRow(resultRow)
    }

    fun wrapTagRow(resultRow: ResultRow): Tag {
        return Tag.wrapRow(resultRow)
    }

    fun filterRow(resultRow: ResultRow): Boolean {
        displayRow(resultRow)
        return true
    }

    fun displayRow(resultRow: ResultRow) {
        logger.debug { "row $resultRow" }
        logger.debug { "${resultRow[BookTable.id]}  ${resultRow[UserBookTable.id]} ${resultRow[UserBookTable.user]}" }
    }

    fun update(updated: Book, book: BookUpdateDto): Book {
        if (!book.title.isNullOrBlank()) {
            updated.title = book.title.trim()
        }
        book.isbn10?.let {
            updated.isbn10 = book.isbn10.trim()
        }
        book.isbn13?.let {
            updated.isbn13 = book.isbn13.trim()
        }
        book.pageCount?.let {
            updated.pageCount = book.pageCount
        }
        book.publisher?.let {
            updated.publisher = book.publisher.trim()
        }
        if (!book.summary.isNullOrBlank()) {
            updated.summary = sanitizeHtml(book.summary)
        }
        // image must be set when saving file succeeds
        book.publishedDate?.let {
            updated.publishedDate = book.publishedDate.trim()
        }
        book.amazonId?.let {
            updated.amazonId = book.amazonId.trim()
        }
        book.goodreadsId?.let {
            updated.goodreadsId = book.goodreadsId.trim()
        }
        book.googleId?.let {
            updated.googleId = book.googleId.trim()
        }
        book.librarythingId?.let {
            updated.librarythingId = book.librarythingId.trim()
        }
        book.language?.let {
            updated.language = book.language.trim()
        }
        updated.modificationDate = nowInstant()
        val authorsList = mutableListOf<Author>()
        book.authors?.forEach {
            // first try to find exact match by id
            var authorEntity: Author? = if (it.id != null) {
                findAuthorsById(it.id)
            }
            // if no id provided or research by id doesn't return anything try to find by name
            else {
                findAuthorsByName(it.name.trim()).firstOrNull()
            }
            if (authorEntity != null) {
                authorsList.add(authorEntity)
            } else {
                authorsList.add(save(it))
            }
        }
        if (authorsList.isNotEmpty()) {
            updated.authors = SizedCollection(authorsList)
        }
        val translatorsList = mutableListOf<Author>()
        book.translators?.forEach {
            // first try to find exact match by id
            var authorEntity: Author? = if (it.id != null) {
                findAuthorsById(it.id)
            }
            // if no id provided or research by id doesn't return anything try to find by name
            else {
                findAuthorsByName(it.name.trim()).firstOrNull()
            }
            if (authorEntity != null) {
                translatorsList.add(authorEntity)
            } else {
                translatorsList.add(save(it))
            }
        }
        if (translatorsList.isNotEmpty()) {
            updated.translators = SizedCollection(translatorsList)
        }
        val tagsList = mutableListOf<Tag>()
        book.tags?.forEach {
            // first try to find exact match by id
            val tagEntity: Tag? = if (it.id != null) {
                findTagById(it.id)
            }
            // if no id provided or research by id doesn't return anything try to find by name
            else {
                findTagsByName(it.name.trim()).firstOrNull()
            }
            if (tagEntity != null) {
                tagsList.add(tagEntity)
            } else {
                tagsList.add(save(it))
            }
        }
        if (tagsList.isNotEmpty()) {
            updated.tags = SizedCollection(tagsList)
        }
        book.series?.forEach {
            // first try to find exact match by id
            var seriesEntity: Series? = if (it.seriesId != null) {
                findSeriesById(it.seriesId)
            }
            // if no id provided or research by id doesn't return anything try to find by name
            else {
                findSeriesByName(it.name.trim()).firstOrNull()
            }
            if (seriesEntity != null) {
                val alreadyExistingOnBook = updated.seriesAndOrder.find { s -> s.series.id == seriesEntity.id }
                // this series is not yet on the book, save a new BookSeriesItem
                if (alreadyExistingOnBook == null) {
                    save(it, seriesEntity, updated)
                } else {
                    // juste update the already exsting BookSeriesItem to avoid duplicates
                    update(it, seriesEntity, updated)
                }
            } else {
                save(it, updated)
            }
        }
        // remove preexisting series which are not in the update dto series
        // which means they are no longer wanted
        updated.seriesAndOrder.forEach { bsi ->
            if (book.series?.find { s -> s.name == bsi.series.name } == null) {
                logger.trace { "delete previously existing series not in updated list ${bsi.series.name}" }
                BookSeries.deleteWhere { BookSeries.series eq(bsi.series.id) }
            }
        }
        return updated
    }

    fun update(bookId: UUID, book: BookUpdateDto): Book {
        val found: Book = Book[bookId]
        return update(found, book)
    }

    fun update(userBookId: UUID, book: UserBookUpdateDto): UserBook {
        val found: UserBook = UserBook[userBookId]
        if (book.owned != null) {
            found.owned = book.owned
        }
        book.personalNotes?.let {
            found.personalNotes = book.personalNotes.trim()
        }
        if (book.toRead != null) {
            found.toRead = book.toRead
        }
        if (book.percentRead != null) {
            found.percentRead = book.percentRead
        }
        if (book.currentPageNumber != null) {
            found.currentPageNumber = book.currentPageNumber
            val current = book.currentPageNumber
            val total = found.book.pageCount
            if (total != null) {
                if (current >= total) {
                    found.percentRead = 100
                } else {
                    found.percentRead = current.times(100).div(total)
                }
            }
        }
        if (book.book != null) {
            update(found.book, fromBookCreateDto(book.book))
        }
        if (book.lastReadingEvent != null) {
            readingEventRepository.save(
                found,
                CreateReadingEventDto(
                    eventType = book.lastReadingEvent,
                    bookId = null,
                    eventDate = null,
                    startDate = null,
                ),
            )
        }
        if (book.borrowed != null) {
            found.borrowed = book.borrowed
        }
        found.modificationDate = nowInstant()
        return found
    }

    fun updateAuthor(authorId: UUID, author: AuthorUpdateDto): Author {
        val found: Author = Author[authorId]
        if (!author.name.isNullOrBlank()) {
            found.name = author.name.trim()
        }
        author.biography?.let {
            found.biography = author.biography.trim()
        }
        author.dateOfDeath?.let {
            found.dateOfDeath = author.dateOfDeath.trim()
        }
        author.dateOfBirth?.let {
            found.dateOfBirth = author.dateOfBirth.trim()
        }
        author.notes?.let {
            found.notes = author.notes.trim()
        }
        author.officialPage?.let {
            found.officialPage = author.officialPage.trim()
        }
        author.wikipediaPage?.let {
            found.wikipediaPage = author.wikipediaPage.trim()
        }
        author.goodreadsPage?.let {
            found.goodreadsPage = author.goodreadsPage.trim()
        }
        author.twitterPage?.let {
            found.twitterPage = author.twitterPage.trim()
        }
        author.facebookPage?.let {
            found.facebookPage = author.facebookPage.trim()
        }
        author.instagramPage?.let {
            found.instagramPage = author.instagramPage.trim()
        }
        // found.image = author.image?.trim()
        found.modificationDate = nowInstant()
        return found
    }

    fun updateSeries(seriesId: UUID, series: SeriesUpdateDto): Series {
        val found: Series = Series[seriesId]
        if (series.name.isNotBlank()) {
            found.name = series.name.trim()
        }
        found.modificationDate = nowInstant()
        return found
    }

    fun save(book: BookCreateDto): Book {
        val authorsList = mutableListOf<Author>()
        book.authors?.forEach { authorDto ->
            val authorEntity: Author? = findAuthorsByName(authorDto.name.trim()).firstOrNull()
            if (authorEntity != null) {
                // we can receive the same author or the same but
                // with only one letter with a different case
                // so do not put twice the same entity in the list
                if (!authorsList.contains(authorEntity)) {
                    authorsList.add(authorEntity)
                }
            } else {
                authorsList.add(save(authorDto))
            }
        }
        val translatorsList = mutableListOf<Author>()
        book.translators?.forEach { authorDto ->
            val authorEntity: Author? = findAuthorsByName(authorDto.name.trim()).firstOrNull()
            if (authorEntity != null) {
                // we can receive the same author or the same but
                // with only one letter with a different case
                // so do not put twice the same entity in the list
                if (!translatorsList.contains(authorEntity)) {
                    translatorsList.add(authorEntity)
                }
            } else {
                translatorsList.add(save(authorDto))
            }
        }
        val tagsList = mutableListOf<Tag>()
        book.tags?.forEach {
            val tagEntity: Tag? = findTagsByName(it.name.trim()).firstOrNull()
            if (tagEntity != null) {
                if (!tagsList.contains(tagEntity)) {
                    tagsList.add(tagEntity)
                }
            } else {
                tagsList.add(save(it))
            }
        }

        val created = Book.new(UUID.randomUUID()) {
            this.title = book.title.trim()
            val instant: Instant = nowInstant()
            this.creationDate = instant
            this.modificationDate = instant
            this.summary = sanitizeHtml(book.summary)
            this.isbn10 = cleanString(book.isbn10)
            this.isbn13 = cleanString(book.isbn13)
            this.pageCount = book.pageCount
            this.publishedDate = cleanString(book.publishedDate)
            this.publisher = cleanString(book.publisher)
            this.image = cleanString(book.image)
            this.amazonId = cleanString(book.amazonId)
            this.goodreadsId = cleanString(book.goodreadsId)
            this.googleId = cleanString(book.googleId)
            this.librarythingId = cleanString(book.librarythingId)
            this.language = cleanString(book.language)
        }
        book.series?.forEach {
            val seriesEntity: Series? = findSeriesByName(it.name.trim()).firstOrNull()
            if (seriesEntity != null) {
                save(it, seriesEntity, created)
            } else {
                save(it, created)
            }
        }

        created.authors = SizedCollection(authorsList)
        created.tags = SizedCollection(tagsList)
        created.translators = SizedCollection(translatorsList)

        // eager loading, see if we keep this in the long term
        created.load(Book::authors)
        created.load(Book::translators)
        created.load(Book::tags)
        return created
    }

    fun cleanString(input: String?): String? {
        if (input.isNullOrBlank()) {
            return null
        }
        return input.trim()
    }

    fun save(author: AuthorDto): Author {
        val created = Author.new {
            name = author.name.trim()
            image = cleanString(author.image)
            dateOfBirth = cleanString(author.dateOfBirth)
            dateOfDeath = cleanString(author.dateOfDeath)
            biography = cleanString(author.biography)
            val instant: Instant = nowInstant()
            creationDate = instant
            modificationDate = instant
            notes = cleanString(author.notes)
            officialPage = cleanString(author.officialPage)
            wikipediaPage = cleanString(author.wikipediaPage)
            goodreadsPage = cleanString(author.goodreadsPage)
            twitterPage = cleanString(author.twitterPage)
            facebookPage = cleanString(author.facebookPage)
            instagramPage = cleanString(author.instagramPage)
        }
        return created
    }

    fun save(tag: TagDto): Tag {
        val created = Tag.new(UUID.randomUUID()) {
            name = tag.name.trim()
            val instant: Instant = nowInstant()
            creationDate = instant
            modificationDate = instant
        }
        return created
    }

    fun saveSeries(series: SeriesUpdateDto): Series {
        val created = Series.new(UUID.randomUUID()) {
            this.name = series.name.trim()
            val instant: Instant = nowInstant()
            creationDate = instant
            modificationDate = instant
        }
        return created
    }

    fun save(series: SeriesOrderDto, book: Book): BookSeriesItem {
        val created = Series.new(UUID.randomUUID()) {
            name = series.name.trim()
            val instant: Instant = nowInstant()
            creationDate = instant
            modificationDate = instant
        }
        val withPosition = BookSeriesItem.new(UUID.randomUUID()) {
            this.series = created
            this.book = book
            this.numberInSeries = series.numberInSeries
        }
        return withPosition
    }

    fun save(series: SeriesOrderDto, seriesEntity: Series, book: Book): BookSeriesItem? {
        val existing =
            BookSeriesItem.find { BookSeries.book eq book.id and (BookSeries.series eq seriesEntity.id) and (BookSeries.numberInSeries eq series.numberInSeries) }
        if (existing.empty()) {
            val withPosition = BookSeriesItem.new(UUID.randomUUID()) {
                this.series = seriesEntity
                this.book = book
                this.numberInSeries = series.numberInSeries
            }
            return withPosition
        }
        return null
    }

    fun update(series: SeriesOrderDto, seriesEntity: Series, book: Book): BookSeriesItem? {
        val existing =
            BookSeriesItem.find { BookSeries.book eq book.id and (BookSeries.series eq seriesEntity.id) }
        if (!existing.empty()) {
            val item = existing.firstOrNull()
            if (item != null) {
                item.numberInSeries = series.numberInSeries
            }
            return item
        }
        return null
    }

    fun save(book: Book, user: User, createUserBookDto: CreateUserBookDto): UserBook {
        val instant: Instant = nowInstant()
        return UserBook.new(UUID.randomUUID()) {
            this.creationDate = instant
            this.modificationDate = instant
            this.user = user
            this.book = book
            this.owned = createUserBookDto.owned
            this.toRead = createUserBookDto.toRead
            this.personalNotes = cleanString(createUserBookDto.personalNotes)
            this.percentRead = createUserBookDto.percentRead
            this.borrowed = createUserBookDto.borrowed
        }
    }

    fun findUserBookById(userbookId: UUID): UserBook = UserBook[userbookId]

    fun findUserBookByCriteria(
        userID: UUID,
        bookId: UUID?,
        eventTypes: List<ReadingEventType>?,
        toRead: Boolean?,
        owned: Boolean?,
        borrowed: Boolean?,
        pageable: Pageable,
    ): PageImpl<UserBook> {
        val cols = mutableListOf<Expression<*>>()
        cols.addAll(UserBookTable.columns)
        cols.addAll(BookTable.columns)
        // avg for all reviews
        val reviewAlias = ReviewTable.alias("rvt")
        val ratingAlias = reviewAlias[ReviewTable.rating].avg().alias("avgRating")
        // avg for current user reviews (thanks to additionalConstraint in JOIN below)
        val userRatingAlias = ReviewTable.rating.avg().alias("usrAvgRating")
        cols.add(ratingAlias)
        cols.add(userRatingAlias)
        val query: Query = UserBookTable.join(BookTable, JoinType.LEFT)
            .join(ReviewTable, JoinType.LEFT, additionalConstraint = { ReviewTable.book eq BookTable.id and(ReviewTable.user eq userID) })
            .join(reviewAlias, JoinType.LEFT, onColumn = reviewAlias[ReviewTable.book], otherColumn = BookTable.id)
            .slice(cols).selectAll()
            .andWhere { UserBookTable.user eq userID }
            .groupBy(UserBookTable.id)
            .withDistinct(true)
        if (bookId != null) {
            query.andWhere { UserBookTable.book eq bookId }
        }
        if (!eventTypes.isNullOrEmpty()) {
            query.andWhere { UserBookTable.lastReadingEvent inList eventTypes }
        }
        if (toRead != null) {
            if (toRead) {
                query.andWhere { UserBookTable.toRead eq toRead }
            } else {
                // default value if checkbox not set is null, so if caller asks explicitly with toRead == false,
                // try to return everything that is not true
                query.andWhere { UserBookTable.toRead eq toRead or (UserBookTable.toRead.isNull()) }
            }
        }
        if (owned != null) {
            if (owned) {
                query.andWhere { UserBookTable.owned eq owned }
            } else {
                // default value if checkbox not set is null, so if caller asks explicitly with owned == false,
                // try to return everything that is not true
                query.andWhere { UserBookTable.owned eq owned or (UserBookTable.owned.isNull()) }
            }
        }
        if (borrowed != null) {
            if (borrowed) {
                query.andWhere { UserBookTable.borrowed eq borrowed }
            } else {
                // default value if checkbox not set is null, so if caller asks explicitly with borrowed == false,
                // try to return everything that is not true
                query.andWhere { UserBookTable.borrowed eq borrowed or (UserBookTable.borrowed.isNull()) }
            }
        }
        val total = query.count()
        if (checkIfRandomSorting(pageable)) {
            query.limit(pageable.pageSize)
            query.orderBy(Random())
        } else {
            query.limit(pageable.pageSize, pageable.offset)
            val orders: Array<Pair<Expression<*>, SortOrder>> = parseSorts(pageable.sort, Pair(UserBookTable.lastReadingEventDate, SortOrder.DESC_NULLS_LAST), cols)
            query.orderBy(*orders)
        }
        val res = query.map { resultRow -> wrapUserBookRow(resultRow, ratingAlias, userRatingAlias) }
        return PageImpl(
            res,
            pageable,
            total,
        )
    }

    private fun wrapUserBookRow(
        resultRow: ResultRow,
        ratingAlias: ExpressionAlias<BigDecimal?>,
        userRatingAlias: ExpressionAlias<BigDecimal?>,
    ): UserBook {
        val r = UserBook.wrapRow(resultRow)
        val rating = resultRow[ratingAlias]
        r.avgRating = rating?.toDouble()
        r.userAvgRating = resultRow[userRatingAlias]?.toDouble()
        return r
    }

    // FIXME put a limit on the number of ids that can be passed for modification
    fun bulkEditUserbooks(userBookBulkUpdateDto: UserBookBulkUpdateDto): Int {
        var tagAdded = 0
        // short circuit if no list operations is needed
        if (!userBookBulkUpdateDto.removeTags.isNullOrEmpty() || !userBookBulkUpdateDto.addTags.isNullOrEmpty()) {
            userBookBulkUpdateDto.ids.forEach {
                val userbook = UserBook[it]
                if (!userBookBulkUpdateDto.removeTags.isNullOrEmpty()) {
                    deleteTagsFromBook(userbook.book.id.value, userBookBulkUpdateDto.removeTags)
                }
                if (!userBookBulkUpdateDto.addTags.isNullOrEmpty()) {
                    tagAdded = addTagsToBook(userbook.book.id.value, userBookBulkUpdateDto.addTags)
                }
            }
        }
        // otherwise : Can't prepare UPDATE statement without fields to update
        if (userBookBulkUpdateDto.owned == null && userBookBulkUpdateDto.toRead == null) {
            return tagAdded
        }
        // SQLite doesn't support LIMIT in DELETE clause., dialect: sqlite.
        // UPDATE with a join clause is unsupported, dialect: sqlite.
        return UserBookTable
            .update(
                where = {
                    UserBookTable.id inList userBookBulkUpdateDto.ids
                },
                body = {
                    val now = nowInstant()
                    var modified = false
                    if (userBookBulkUpdateDto.toRead != null) {
                        it[UserBookTable.toRead] = userBookBulkUpdateDto.toRead
                        modified = true
                    }
                    if (userBookBulkUpdateDto.owned != null) {
                        it[UserBookTable.owned] = userBookBulkUpdateDto.owned
                        modified = true
                    }
                    if (modified) {
                        it[UserBookTable.modificationDate] = now
                    }
                },
            )
    }

    fun deleteUserBookById(userbookId: UUID) {
        val entity: UserBook = UserBook[userbookId]
        entity.delete()
    }

    fun deleteBookById(bookId: UUID) {
        val book = Book[bookId]
        book.image?.let { fileManager.deleteImage(it) }
        book.delete()
    }

    fun deleteTagFromBook(bookId: UUID, tagId: UUID) {
        BookTags.deleteWhere {
            BookTags.tag eq tagId and(BookTags.book eq bookId)
        }
    }

    fun deleteSeriesFromBook(bookId: UUID, seriesId: UUID) {
        BookSeries.deleteWhere {
            series eq seriesId and(book eq bookId)
        }
    }

    fun deleteTagsFromBook(bookId: UUID, tagIds: List<UUID>) {
        BookTags.deleteWhere {
            BookTags.tag inList tagIds and(BookTags.book eq bookId)
        }
    }

    fun addTagsToBook(bookId: UUID, tagIds: List<UUID>): Int {
        // first remove ids of already existing BookTags to prevent duplicate keys constraints exceptions
        // val toInsert = tagIds.filter {
        //     BookTags.select { BookTags.tag eq it and(BookTags.book eq bookId) }.toList().isEmpty()
        // }
        val created = BookTags.batchInsert(tagIds, ignore = true) { tagId ->
            this[BookTags.book] = bookId
            this[BookTags.tag] = tagId
        }
        return created.size
    }

    fun deleteTagById(tagId: UUID) {
        Tag[tagId].delete()
    }

    fun deleteSeriesById(seriesId: UUID) {
        Series[seriesId].delete()
    }

    /**
     * Removes an author from a book without deleting the author from the database.
     * Used to clean the composite table
     */
    fun deleteAuthorFromBook(bookId: UUID, authorId: UUID) {
        BookAuthors.deleteWhere {
            BookAuthors.book eq bookId and(BookAuthors.author eq authorId)
        }
    }

    fun deleteAuthorById(authorId: UUID) {
        Author[authorId].delete()
    }

    /**
     * Removes a translator from a book without deleting the translator from the database.
     * Used to clean the composite table
     */
    fun deleteTranslatorFromBook(bookId: UUID, translatorId: UUID) {
        BookTranslators.deleteWhere {
            BookTranslators.book eq bookId and(BookTranslators.translator eq translatorId)
        }
    }

    fun booksWithSeries(pageable: Pageable): Page<Book> {
        val query = BookTable
            .slice(BookTable.columns)
            .select { BookTable.seriesBak.isNotNull() }
            .withDistinct()

        val total = query.count()
        query.limit(pageable.pageSize, pageable.offset)
        val orders: Array<Pair<Expression<*>, SortOrder>> = parseSorts(pageable.sort, Pair(BookTable.title, SortOrder.ASC_NULLS_LAST), BookTable)
        query.orderBy(*orders)
        return PageImpl(
            query.map { resultRow -> wrapRow(resultRow) },
            pageable,
            total,
        )
    }

    fun updateSeriesFromStringToDedicatedTable(book: Book) {
        val seriesBak = book.seriesBak?.trim()
        if (!seriesBak.isNullOrBlank()) {
            val seriesEntity: Series? = findSeriesByName(seriesBak).firstOrNull()
            if (seriesEntity != null) {
                save(
                    SeriesOrderDto(
                        name = seriesBak,
                        numberInSeries = book.numberInSeries,
                    ),
                    seriesEntity,
                    book,
                )
            } else {
                save(
                    SeriesOrderDto(
                        name = seriesBak,
                        numberInSeries = book.numberInSeries,
                    ),
                    book,
                )
            }
        }
    }
}
