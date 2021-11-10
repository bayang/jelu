package io.github.bayang.jelu.service

import io.github.bayang.jelu.dao.User
import io.github.bayang.jelu.dao.UserRepository
import io.github.bayang.jelu.dto.CreateUserDto
import io.github.bayang.jelu.dto.UserDto
import io.github.bayang.jelu.dto.UserDtoWithEvents
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Component
class UserService(private val userRepository: UserRepository) {

    @Transactional
    fun findAll(searchTerm: String?): List<UserDto> = userRepository.findAll(searchTerm).map { it.toUserDto() }

    @Transactional
    fun findByEmailIgnoreCase(email: String): List<UserDto> = userRepository.findByEmailIgnoreCase(email).map { it.toUserDto() }

    @Transactional
    fun findUserById(id: UUID): UserDtoWithEvents = User[id].toUserDtoWithEvents()

    @Transactional
    fun save(user: CreateUserDto): UserDto = userRepository.save(user).toUserDto()

}