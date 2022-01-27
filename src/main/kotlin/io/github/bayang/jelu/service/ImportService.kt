package io.github.bayang.jelu.service

import io.github.bayang.jelu.dao.ImportEntity
import io.github.bayang.jelu.dao.ImportRepository
import io.github.bayang.jelu.dao.ProcessingStatus
import io.github.bayang.jelu.dto.ImportDto
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class ImportService(private val importRepository: ImportRepository) {

    @Transactional
    fun save(
        entity: ImportDto,
        processingStatus: ProcessingStatus,
        userId: UUID,
        shouldFetchMetadata: Boolean
    ) {
        importRepository.save(entity, processingStatus, userId, shouldFetchMetadata)
    }

    @Transactional
    fun updateStatus(
        oldStatus: ProcessingStatus,
        newStatus: ProcessingStatus,
        userId: UUID
    ): Int = importRepository.updateStatus(oldStatus, newStatus, userId)

    @Transactional
    fun getByprocessingStatusAndUser(
        processingStatus: ProcessingStatus,
        userId: UUID
    ): List<ImportEntity> = importRepository.getByprocessingStatusAndUser(processingStatus, userId)

    @Transactional
    fun updateStatus(entityId: UUID, newStatus: ProcessingStatus): Int = importRepository.updateStatus(entityId, newStatus)

    @Transactional
    fun countByprocessingStatusAndUser(
        processingStatus: ProcessingStatus,
        userId: UUID
    ): Long = importRepository.countByprocessingStatusAndUser(processingStatus, userId)
}
