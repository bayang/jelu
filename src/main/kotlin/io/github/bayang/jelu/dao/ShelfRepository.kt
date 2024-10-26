package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.CreateShelfDto
import io.github.bayang.jelu.dto.UserDto
import io.github.bayang.jelu.utils.nowInstant
import mu.KotlinLogging
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Repository
class ShelfRepository {

    fun save(createShelfDto: CreateShelfDto, user: UserDto): Shelf {
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
    ): List<Shelf> {
        val query = ShelfTable.selectAll()
        user?.let {
            query.andWhere { ShelfTable.user eq user.id }
        }
        name?.let {
            query.andWhere { ShelfTable.name like(formatLike(name)) }
        }
        targetId?.let {
            query.andWhere { ShelfTable.targetId eq(targetId) }
        }
        return Shelf.wrapRows(query).toList()
    }

    fun findById(
        id: UUID,
    ): Shelf = Shelf[id]

    fun delete(shelfId: UUID) {
        Shelf[shelfId].delete()
    }
}
