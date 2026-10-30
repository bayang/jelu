package io.github.bayang.jelu.service.exports

import io.github.bayang.jelu.dao.UserExport
import io.github.bayang.jelu.dao.UserExportRepository
import io.github.bayang.jelu.dto.UserDto
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Component
class UserExportService(
    private val userExportRepository: UserExportRepository,
) {
    @Transactional
    fun existsByNameAndOwner(
        user: UserDto,
        name: String,
    ): Long = userExportRepository.existsByNameAndOwner(user, name)

    @Transactional
    fun save(
        name: String,
        user: UserDto,
    ): UserExport = userExportRepository.save(name, user)

    @Transactional
    fun delete(id: UUID) {
        userExportRepository.delete(id)
    }

    @Transactional
    fun findOlderReports(): List<UserExport> = userExportRepository.findOlderExports()

    @Transactional
    fun deleteAll(): Int = userExportRepository.deleteAll()
}
