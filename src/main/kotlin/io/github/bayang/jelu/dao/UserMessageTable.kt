package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.UserMessageDto
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.javatime.timestamp
import java.util.UUID

object UserMessageTable : UUIDTable("user_message") {
    val creationDate = timestamp("creation_date")
    val modificationDate = timestamp("modification_date")
    val user = reference("user", UserTable, onDelete = ReferenceOption.CASCADE)
    val messageCategory = enumerationByName("category", 200, MessageCategory::class)
    val message: Column<String?> = varchar("message", 50000).nullable()
    val link: Column<String?> = varchar("link", 50000).nullable()
    val read: Column<Boolean> = bool("read")
}

class UserMessage(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<UserMessage>(UserMessageTable)

    var creationDate by UserMessageTable.creationDate
    var modificationDate by UserMessageTable.modificationDate
    var user by User referencedOn UserMessageTable.user
    var messageCategory by UserMessageTable.messageCategory
    var message by UserMessageTable.message
    var link by UserMessageTable.link
    var read by UserMessageTable.read

    fun toUserMessageDto(): UserMessageDto =
        UserMessageDto(
            id = this.id.value,
            message = this.message,
            creationDate = this.creationDate,
            modificationDate = this.modificationDate,
            link = this.link,
            read = this.read,
            category = this.messageCategory,
        )
}

enum class MessageCategory {
    SUCCESS,
    INFO,
    WARNING,
    ERROR,
}
