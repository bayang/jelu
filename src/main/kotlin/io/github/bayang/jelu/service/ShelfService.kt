package io.github.bayang.jelu.service

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dao.ShelfRepository
import io.github.bayang.jelu.dto.CreateShelfDto
import io.github.bayang.jelu.dto.ShelfDto
import io.github.bayang.jelu.dto.UserDto
import io.github.bayang.jelu.errors.JeluValidationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class ShelfService(
    private val shelfRepository: ShelfRepository,
    private val properties: JeluProperties,
) {
    @Transactional
    fun save(
        createShelfDto: CreateShelfDto,
        user: UserDto,
    ): ShelfDto {
        val maybeDuplicate = find(user, createShelfDto.name, null, Pageable.ofSize(10))
        if (maybeDuplicate.totalElements > 0) {
            throw JeluValidationException("Shelf with name ${createShelfDto.name} already exists")
        }
        return shelfRepository.save(createShelfDto, user).toShelfDto()
    }

    @Transactional
    fun find(
        user: UserDto?,
        name: String?,
        targetId: UUID?,
        pageable: Pageable,
    ): Page<ShelfDto> = shelfRepository.find(user, name, targetId, pageable).map { it.toShelfDto() }

    @Transactional
    fun findById(id: UUID): ShelfDto = shelfRepository.findById(id).toShelfDto()

    @Transactional
    fun delete(shelfId: UUID) {
        shelfRepository.delete(shelfId)
    }
}
