package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dao.Book.Companion.referrersOn
import io.github.bayang.jelu.dto.UserDto
import io.github.bayang.jelu.dto.UserDtoWithEvents
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
    val email: Column<String> = varchar("email", 200)
    val password: Column<String> = varchar("password", 1000)
    val isAdmin: Column<Boolean> = bool("is_admin")
}

class User(id: EntityID<UUID>): UUIDEntity(id) {
    fun toUserDto(): UserDto = UserDto(
        id = this.id.value,
        creationDate = this.creationDate,
        modificationDate = this.modificationDate,
        email = this.email,
        password = "****",
        isAdmin = this.isAdmin,
    )
//    fun toUserDtoWithEvents(): UserDtoWithEvents = UserDtoWithEvents(
//        id = this.id.value,
//        creationDate = this.creationDate,
//        modificationDate = this.modificationDate,
//        email = this.email,
//        password = "****",
//        isAdmin = this.isAdmin,
//        readingEvents = this.userBooks.fla
//    )

    companion object : UUIDEntityClass<User>(UserTable)
    var creationDate by UserTable.creationDate
    var modificationDate by UserTable.modificationDate
    var email by UserTable.email
    var password by UserTable.password
    var isAdmin by UserTable.isAdmin
    val userBooks by UserBook referrersOn UserBookTable.book
//    val readingEvents by ReadingEvent referrersOn ReadingEventTable.user // make sure to use val and referrersOn
}