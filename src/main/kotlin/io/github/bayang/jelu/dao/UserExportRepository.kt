package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.UserDto
import io.github.bayang.jelu.utils.nowInstant
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Repository
class UserExportRepository {
    fun save(
        name: String,
        user: UserDto,
    ): UserExport {
        val instant: Instant = nowInstant()
        return UserExport.new {
            this.creationDate = instant
            this.modificationDate = instant
            this.user = User[user.id!!]
            this.name = name
        }
    }

    fun existsByNameAndOwner(
        user: UserDto,
        name: String,
    ): Long =
        UserExportTable
            .join(UserTable, JoinType.LEFT)
            .selectAll()
            .where { UserExportTable.user eq user.id }
            .andWhere { UserExportTable.name eq name }
            .count()

    fun delete(userExportId: UUID) {
        UserExport[userExportId].delete()
    }

    fun findOlderExports(): List<UserExport> {
        val q =
            UserExportTable
                .selectAll()
                .where { UserExportTable.creationDate less nowInstant().minus(12, ChronoUnit.MINUTES) }
                .limit(50)
        return UserExport.wrapRows(q).toList()
    }

    fun deleteAll(): Int = UserExportTable.deleteAll()
}
