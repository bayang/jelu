package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dao.MessageCategory
import io.github.bayang.jelu.dto.CreateUserMessageDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.dto.UpdateUserMessageDto
import io.github.bayang.jelu.dto.UserMessageDto
import io.github.bayang.jelu.service.UserMessageService
import io.swagger.v3.oas.annotations.Hidden
import jakarta.validation.Valid
import mu.KotlinLogging
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1")
class UserMessagesController(
    private val userMessageService: UserMessageService,
    private val properties: JeluProperties,
) {

    @GetMapping(path = ["/user-messages"])
    fun userMessages(
        @RequestParam(name = "messageCategories", required = false) messageCategories: List<MessageCategory>?,
        @RequestParam(name = "read", required = false) read: Boolean?,
        @PageableDefault(page = 0, size = 20, direction = Sort.Direction.DESC, sort = ["modificationDate"]) @ParameterObject pageable: Pageable,
        principal: Authentication,
    ): Page<UserMessageDto> = userMessageService.find((principal.principal as JeluUser).user, read, messageCategories, pageable)

    @PutMapping(path = ["/user-messages/{id}"])
    fun updateMessage(
        @PathVariable("id")
        messageId: UUID,
        @RequestBody
        @Valid
        updateDto: UpdateUserMessageDto,
    ): UserMessageDto {
        return userMessageService.update(messageId, updateDto)
    }

    @Hidden
    @PostMapping(path = ["/user-messages"])
    fun createMessage(
        @RequestBody @Valid
        createUserMessageDto: CreateUserMessageDto,
        principal: Authentication,
    ): UserMessageDto {
        return userMessageService.save(createUserMessageDto, (principal.principal as JeluUser).user)
    }
}
