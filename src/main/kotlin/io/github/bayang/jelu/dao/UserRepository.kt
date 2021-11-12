package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.CreateUserDto
import io.github.bayang.jelu.errors.JeluException
import io.github.bayang.jelu.utils.nowInstant
import mu.KotlinLogging
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.lowerCase
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

private val logger = KotlinLogging.logger {}

@Repository
class UserRepository {

    fun findAll(searchTerm: String?): SizedIterable<User> {
        return if (! searchTerm.isNullOrBlank()) {
            User.find { UserTable.email like searchTerm }
        } else {
            User.all()
        }
    }

    fun countUsers(): Long = User.count()

    fun findByEmailIgnoreCase(email: String): SizedIterable<User> =
        User.find { UserTable.email.lowerCase() eq email.lowercase() }

    fun findUserById(id: UUID): User = User[id]

    fun save(user: CreateUserDto): User {
        val created = User.new{
            email = user.email
            val instant: Instant = nowInstant()
            creationDate = instant
            modificationDate = instant
            password = user.password
            isAdmin = user.isAdmin
        }
        return created
    }
}
