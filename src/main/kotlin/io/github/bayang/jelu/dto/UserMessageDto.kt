package io.github.bayang.jelu.dto

import io.github.bayang.jelu.dao.MessageCategory
import java.time.Instant
import java.util.UUID

data class CreateUserMessageDto(
    val message: String?,
    val link: String?,
    val category: MessageCategory,
)

data class UpdateUserMessageDto(
    val message: String?,
    val link: String?,
    val category: MessageCategory?,
    val read: Boolean?,
)

data class UserMessageDto(
    val id: UUID?,
    val message: String?,
    val link: String?,
    val category: MessageCategory,
    val read: Boolean,
    val creationDate: Instant?,
    val modificationDate: Instant?,
)
