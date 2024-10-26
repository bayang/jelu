package io.github.bayang.jelu.service

import io.github.bayang.jelu.dao.MessageCategory
import io.github.bayang.jelu.dao.UserMessageRepository
import io.github.bayang.jelu.dto.CreateUserMessageDto
import io.github.bayang.jelu.dto.UpdateUserMessageDto
import io.github.bayang.jelu.dto.UserDto
import io.github.bayang.jelu.dto.UserMessageDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class UserMessageService(private val userMessageRepository: UserMessageRepository) {

    @Transactional
    fun save(createUserMessageDto: CreateUserMessageDto, user: UserDto): UserMessageDto {
        return userMessageRepository.save(createUserMessageDto, user).toUserMessageDto()
    }

    @Transactional
    fun find(
        user: UserDto,
        read: Boolean?,
        messageCategories: List<MessageCategory>?,
        pageable: Pageable,
    ): Page<UserMessageDto> {
        return userMessageRepository.find(user, read, messageCategories, pageable).map { it.toUserMessageDto() }
    }

    @Transactional
    fun update(userMessageId: UUID, updateDto: UpdateUserMessageDto): UserMessageDto {
        return userMessageRepository.update(userMessageId, updateDto).toUserMessageDto()
    }

    @Transactional
    fun delete(userMessageId: UUID) {
        userMessageRepository.delete(userMessageId)
    }
}
