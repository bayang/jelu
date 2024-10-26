package io.github.bayang.jelu.service

import io.github.bayang.jelu.dao.ShelfRepository
import io.github.bayang.jelu.dto.CreateShelfDto
import io.github.bayang.jelu.dto.ShelfDto
import io.github.bayang.jelu.dto.UserDto
import io.github.bayang.jelu.errors.JeluValidationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class ShelfService(
    private val shelfRepository: ShelfRepository,
) {

    @Transactional
    fun save(createShelfDto: CreateShelfDto, user: UserDto): ShelfDto {
        val userShelves = find(user, null, null)
        if (userShelves.size >= 10) {
            throw JeluValidationException("Maximum number of shelves reaches")
        }
        return shelfRepository.save(createShelfDto, user).toShelfDto()
    }

    @Transactional
    fun find(
        user: UserDto?,
        name: String?,
        targetId: UUID?,
    ): List<ShelfDto> {
        return shelfRepository.find(user, name, targetId).map { it.toShelfDto() }
    }

    @Transactional
    fun findById(
        id: UUID,
    ): ShelfDto = shelfRepository.findById(id).toShelfDto()

    @Transactional
    fun delete(shelfId: UUID) {
        shelfRepository.delete(shelfId)
    }
}
