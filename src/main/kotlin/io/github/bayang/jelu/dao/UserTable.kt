package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.UserDto
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.*

object UserTable: UUIDTable("user") {
    val creationDate = timestamp("creation_date")
    val modificationDate = timestamp("modification_date")
    val login: Column<String> = varchar("login", 50)
    val password: Column<String> = varchar("password", 1000)
    val isAdmin: Column<Boolean> = bool("is_admin")
}

class User(id: EntityID<UUID>): UUIDEntity(id) {
    fun toUserDto(): UserDto = UserDto(
        id = this.id.value,
        creationDate = this.creationDate,
        modificationDate = this.modificationDate,
        login = this.login,
        password = "****",
        isAdmin = this.isAdmin,
    )

    companion object : UUIDEntityClass<User>(UserTable)
    var creationDate by UserTable.creationDate
    var modificationDate by UserTable.modificationDate
    var login by UserTable.login
    var password by UserTable.password
    var isAdmin by UserTable.isAdmin
    val userBooks by UserBook referrersOn UserBookTable.book
}