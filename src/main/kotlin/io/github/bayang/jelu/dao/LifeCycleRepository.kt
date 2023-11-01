package io.github.bayang.jelu.dao

import io.github.bayang.jelu.utils.nowInstant
import org.springframework.stereotype.Repository

@Repository
class LifeCycleRepository {

    fun findLifeCycle(): LifeCycle {
        return LifeCycle.all().first()
    }

    fun setSeriesMigrated(newValue: Boolean): LifeCycle {
        val lifeCycle = findLifeCycle()
        lifeCycle.seriesMigrated = newValue
        lifeCycle.modificationDate = nowInstant()
        return lifeCycle
    }
}
