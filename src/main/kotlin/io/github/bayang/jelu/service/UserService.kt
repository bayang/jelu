package io.github.bayang.jelu.service

import io.github.bayang.jelu.dao.User
import io.github.bayang.jelu.dao.UserRepository
import io.github.bayang.jelu.dto.*
import io.github.bayang.jelu.errors.JeluException
import mu.KotlinLogging
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

private val logger = KotlinLogging.logger {}

@Component
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
    ): UserDetailsService {

    @Transactional
    fun findAll(searchTerm: String?): List<UserDto> = userRepository.findAll(searchTerm).map { it.toUserDto() }

    @Transactional
    fun findByEmailIgnoreCase(email: String): List<UserDto> = userRepository.findByEmailIgnoreCase(email).map { it.toUserDto() }

    @Transactional
    fun findUserById(id: UUID): UserDtoWithEvents = User[id].toUserDtoWithEvents()

    @Transactional
    fun save(user: CreateUserDto): UserDto {
        if (! userRepository.findByEmailIgnoreCase(user.email).empty()) {
            logger.error { "user already exists ${user.email}" }
            throw JeluException("User already exists ${user.email}")
        }
        return userRepository.save(user.copy(password = passwordEncoder.encode(user.password))).toUserDto()
    }

    @Transactional
    fun isInitialSetup(): Boolean = userRepository.countUsers() < 1L

    @Transactional
    override fun loadUserByUsername(username: String): UserDetails {
        if (userRepository.countUsers() == 0L) {
            return DummyUser(passwordEncoder.encode("initial"))
        }
        userRepository.findByEmailIgnoreCase(username).let {
            return JeluUser(it.first())
        }
    }

}