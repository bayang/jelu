package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.CreateShelfDto
import io.github.bayang.jelu.dto.UserDto
import io.github.bayang.jelu.utils.nowInstant
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.sql.Expression
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
class ShelfRepository {
    fun save(
        createShelfDto: CreateShelfDto,
        user: UserDto,
    ): Shelf {
        val instant: Instant = nowInstant()
        return Shelf.new {
            this.creationDate = instant
            this.modificationDate = instant
            this.user = User[user.id!!]
            this.name = createShelfDto.name
            this.targetId = createShelfDto.targetId
        }
    }

    fun find(
        user: UserDto?,
        name: String?,
        targetId: UUID?,
        pageable: Pageable,
    ): Page<Shelf> {
        val query = ShelfTable.selectAll()
        user?.let {
            query.andWhere { ShelfTable.user eq user.id }
        }
        name?.let {
            query.andWhere { ShelfTable.name like (formatLike(name)) }
        }
        targetId?.let {
            query.andWhere { ShelfTable.targetId eq (targetId) }
        }
        val total = query.count()
        query.limit(pageable.pageSize)
        query.offset(pageable.offset)
        val orders: Array<Pair<Expression<*>, SortOrder>> =
            parseSorts(pageable.sort, Pair(ShelfTable.name, SortOrder.ASC_NULLS_LAST), ShelfTable)
        query.orderBy(*orders)
        return PageImpl(
            query.map { resultRow -> Shelf.wrapRow(resultRow) },
            pageable,
            total,
        )
    }

    fun findById(id: UUID): Shelf = Shelf[id]

    fun delete(shelfId: UUID) {
        Shelf[shelfId].delete()
    }
}
