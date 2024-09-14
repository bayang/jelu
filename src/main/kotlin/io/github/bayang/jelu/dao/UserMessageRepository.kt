package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.CreateUserMessageDto
import io.github.bayang.jelu.dto.UpdateUserMessageDto
import io.github.bayang.jelu.utils.nowInstant
import mu.KotlinLogging
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
class UserMessageRepository {

    fun save(createUserMessageDto: CreateUserMessageDto, user: User): UserMessage {
        val instant: Instant = nowInstant()
        return UserMessage.new {
            this.creationDate = instant
            this.modificationDate = instant
            this.user = user
            this.message = createUserMessageDto.message
            this.link = createUserMessageDto.link
            this.messageCategory = createUserMessageDto.category
            this.read = false
        }
    }

    fun find(
        user: User,
        read: Boolean?,
        messageCategories: List<MessageCategory>?,
        pageable: Pageable,
    ): Page<UserMessage> {
        val query = UserMessageTable.join(UserTable, JoinType.LEFT)
            .selectAll().where { UserMessageTable.user eq user.id }
        if (read != null) {
            query.andWhere { UserMessageTable.read eq read }
        }
        if (messageCategories != null && messageCategories.isNotEmpty()) {
            query.andWhere { UserMessageTable.messageCategory inList messageCategories }
        }
        val total = query.count()
        query.limit(pageable.pageSize, pageable.offset)
        val orders: Array<Pair<Expression<*>, SortOrder>> = parseSorts(pageable.sort, Pair(UserMessageTable.modificationDate, SortOrder.DESC_NULLS_LAST), UserMessageTable)
        query.orderBy(*orders)
        val res = UserMessage.wrapRows(query).toList()
        return PageImpl(
            res,
            pageable,
            total,
        )
    }

    fun update(userMessageId: UUID, updateDto: UpdateUserMessageDto): UserMessage {
        return UserMessage[userMessageId].apply {
            this.modificationDate = nowInstant()
            if (updateDto.read != null) {
                this.read = updateDto.read
            }
            if (!updateDto.message.isNullOrEmpty()) {
                this.message = updateDto.message
            }
            if (!updateDto.link.isNullOrEmpty()) {
                this.link = updateDto.link
            }
            if (updateDto.category != null) {
                this.messageCategory = updateDto.category
            }
        }
    }

    fun delete(userMessageId: UUID) {
        UserMessage[userMessageId].delete()
    }
}
