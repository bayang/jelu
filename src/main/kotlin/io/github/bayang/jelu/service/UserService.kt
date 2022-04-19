package io.github.bayang.jelu.service

import io.github.bayang.jelu.dao.User
import io.github.bayang.jelu.dao.UserRepository
import io.github.bayang.jelu.dto.*
import io.github.bayang.jelu.errors.JeluException
import mu.KotlinLogging
import org.springframework.security.core.session.SessionRegistry
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
    private val sessionRegistry: SessionRegistry,
    private val passwordEncoder: PasswordEncoder
) : UserDetailsService {

    @Transactional
    fun findAll(searchTerm: String?): List<UserDto> = userRepository.findAll(searchTerm).map { it.toUserDto() }

    @Transactional
    fun findByLogin(login: String): List<UserDto> = userRepository.findByLogin(login).map { it.toUserDto() }

    @Transactional
    fun findUserById(id: UUID): UserDto = User[id].toUserDto()

    @Transactional
    fun findUserEntityById(id: UUID): User = User[id]

    @Transactional
    fun save(user: CreateUserDto): UserDto {
        if (! userRepository.findByLogin(user.login.trim()).empty()) {
            logger.error { "user already exists ${user.login}" }
            throw JeluException("User already exists ${user.login}")
        }
        return userRepository.save(user.copy(password = passwordEncoder.encode(user.password.trim()))).toUserDto()
    }

    @Transactional
    fun isInitialSetup(): Boolean = userRepository.countUsers() < 1L

    @Transactional
    override fun loadUserByUsername(username: String): UserDetails {
        if (userRepository.countUsers() == 0L) {
            return DummyUser(passwordEncoder.encode("initial"))
        }
        userRepository.findByLogin(username).let {
            return JeluUser(it.first())
        }
    }

    @Transactional
    fun deleteUser(userId: UUID) {
        userRepository.deleteUser(userId)
    }

    @Transactional
    fun updateUser(userId: UUID, userDto: UpdateUserDto): UserDto {
        val updated = userRepository.updateUser(userId, userDto.copy(password = passwordEncoder.encode(userDto.password.trim()))).toUserDto()
        val jeluUser: JeluUser = this.loadUserByUsername(updated.login) as JeluUser
        sessionRegistry.getAllSessions(jeluUser, false).forEach {
            it.expireNow()
        }
        return updated
    }
}
