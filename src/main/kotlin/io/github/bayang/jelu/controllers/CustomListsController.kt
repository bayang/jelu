package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dto.BookDto
import io.github.bayang.jelu.dto.CustomListDto
import io.github.bayang.jelu.dto.CustomListRemoveDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.errors.JeluAuthenticationException
import io.github.bayang.jelu.service.CustomListService
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import mu.KotlinLogging
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1")
class CustomListsController(
    private val customListService: CustomListService,
    private val properties: JeluProperties,
) {

    @GetMapping(path = ["/custom-lists"])
    fun userMessages(
        @RequestParam(name = "name", required = false) name: String?,
        @PageableDefault(page = 0, size = 20, direction = Sort.Direction.DESC, sort = ["modificationDate"]) @ParameterObject pageable: Pageable,
        principal: Authentication,
    ): Page<CustomListDto> = customListService.find((principal.principal as JeluUser).user.id!!, name, pageable)

    @PostMapping(path = ["/custom-lists"])
    fun createMessage(
        @RequestBody @Valid
        customListDto: CustomListDto,
        principal: Authentication,
    ): CustomListDto {
        return if (customListDto.id != null) {
            customListService.update(customListDto)
        } else {
            customListService.save(customListDto, (principal.principal as JeluUser).user.id!!)
        }
    }

    @PostMapping(path = ["/custom-lists/remove"])
    fun removeBooksFromList(
        @RequestBody @Valid
        customListRemoveDto: CustomListRemoveDto,
    ): ResponseEntity<Unit> {
        customListService.removeBooksFromList(customListRemoveDto)
        return ResponseEntity.noContent().build()
    }

    @GetMapping(path = ["/custom-lists/{id}"])
    fun getList(
        @PathVariable("id") listId: UUID,
        principal: Authentication?,
    ): CustomListDto {
        val list = customListService.findById(listId)
        if (!list.public && principal == null) {
            throw JeluAuthenticationException("Resource unauthorized")
        }
        return list
    }

    @GetMapping(path = ["/custom-lists/{id}/books"])
    fun getListBooks(
        @PathVariable("id") listId: UUID,
        @PageableDefault(page = 0, size = 20, direction = Sort.Direction.DESC, sort = ["title"]) @ParameterObject pageable: Pageable,
        principal: Authentication?,
    ): Page<BookDto> {
        return customListService.findListBooks(listId, pageable, principal)
    }

    @ApiResponse(responseCode = "204", description = "Deleted the Custom list")
    @DeleteMapping(path = ["/custom-lists/{id}"])
    fun deleteListById(@PathVariable("id") listId: UUID): ResponseEntity<Unit> {
        customListService.delete(listId)
        return ResponseEntity.noContent().build()
    }
}
