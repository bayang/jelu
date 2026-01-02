package io.github.bayang.jelu.service

import io.github.bayang.jelu.dao.LifeCycleRepository
import io.github.bayang.jelu.dto.LifeCycleDto
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LifeCycleService(
    private val lifeCycleRepository: LifeCycleRepository,
) {
    @Transactional
    fun getLifeCycle(): LifeCycleDto = lifeCycleRepository.findLifeCycle().toLifeCycleDto()

    @Transactional
    fun setSeriesMigrated(newValue: Boolean = true): LifeCycleDto = lifeCycleRepository.setSeriesMigrated(newValue).toLifeCycleDto()
}
