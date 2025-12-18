package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.CustomListDto
import io.github.bayang.jelu.errors.JeluValidationException
import io.github.bayang.jelu.utils.nowInstant
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Repository
class CustomListRepository {

    fun save(createCustomListDto: CustomListDto, userId: UUID): CustomList {
        val instant: Instant = nowInstant()
        return CustomList.new {
            this.creationDate = instant
            this.modificationDate = instant
            this.user = User[userId]
            this.name = createCustomListDto.name
            this.tags = createCustomListDto.tags
            this.public = createCustomListDto.public
            this.actionable = createCustomListDto.actionable
        }
    }

    fun update(createCustomListDto: CustomListDto): CustomList {
        val instant: Instant = nowInstant()
        if (createCustomListDto.id != null) {
            return CustomList[createCustomListDto.id].apply {
                this.modificationDate = instant
                this.name = createCustomListDto.name
                this.tags = createCustomListDto.tags
                this.actionable = createCustomListDto.actionable
                this.public = createCustomListDto.public
            }
        }
        throw JeluValidationException("missing id")
    }

    fun find(
        user: UUID,
        name: String?,
        pageable: Pageable,
    ): Page<CustomList> {
        val query = CustomListTable.join(UserTable, JoinType.LEFT)
            .selectAll().where { CustomListTable.user eq user }
        if (name != null) {
            query.andWhere { CustomListTable.name eq name }
        }
        val total = query.count()
        query.limit(pageable.pageSize, pageable.offset)
        val orders: Array<Pair<Expression<*>, SortOrder>> = parseSorts(
            pageable.sort,
            Pair(
                CustomListTable.modificationDate,
                SortOrder.DESC_NULLS_LAST,
            ),
            CustomListTable,
        )
        query.orderBy(*orders)
        val res = CustomList.wrapRows(query).toList()
        return PageImpl(
            res,
            pageable,
            total,
        )
    }

    fun findListBooks(
        bookIds: List<String>,
        pageable: Pageable,
    ): Page<Book> {
        val uuids = bookIds.map { UUID.fromString(it) }
        val query = BookTable.selectAll()
            .withDistinct()
            .where { BookTable.id inList uuids }
        val total = query.count()
        query.limit(pageable.pageSize, pageable.offset)
        val orders: Array<Pair<Expression<*>, SortOrder>> = parseSorts(pageable.sort, Pair(BookTable.title, SortOrder.ASC_NULLS_LAST), BookTable)
        query.orderBy(*orders)
        return PageImpl(
            query.map { resultRow -> Book.wrapRow(resultRow) },
            pageable,
            total,
        )
    }

    fun findById(listId: UUID): CustomList = CustomList[listId]

    fun delete(listId: UUID) {
        CustomList[listId].delete()
    }
}
