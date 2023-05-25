package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.CreateUserDto
import io.github.bayang.jelu.dto.UpdateUserDto
import io.github.bayang.jelu.utils.nowInstant
import mu.KotlinLogging
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.and
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Repository
class UserRepository {

    fun findAll(searchTerm: String?): SizedIterable<User> {
        return if (!searchTerm.isNullOrBlank()) {
            User.find { UserTable.login like searchTerm }
        } else {
            User.all()
        }
    }

    fun deleteUser(userId: UUID) {
        User[userId].delete()
    }

    fun countUsers(): Long = User.count()

    fun findByLogin(login: String): SizedIterable<User> =
        User.find { UserTable.login eq login }

    fun findByLoginAndProvider(login: String, provider: Provider): SizedIterable<User> =
        User.find { UserTable.login eq login and(UserTable.provider eq provider) }

    fun findUserById(id: UUID): User = User[id]

    fun save(user: CreateUserDto): User {
        val created = User.new {
            login = user.login
            val instant: Instant = nowInstant()
            creationDate = instant
            modificationDate = instant
            password = user.password
            isAdmin = user.isAdmin
            provider = user.provider
        }
        return created
    }

    fun updateUser(userId: UUID, updateUserDto: UpdateUserDto): User {
        return User[userId].apply {
            this.modificationDate = nowInstant()
            this.password = updateUserDto.password
            if (updateUserDto.isAdmin != null) {
                this.isAdmin = updateUserDto.isAdmin
            }
            if (updateUserDto.provider != null) {
                this.provider = updateUserDto.provider
            }
        }
    }
}
