package io.github.bayang.jelu.config

import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

private val logger = KotlinLogging.logger {}

const val HIKARI_POOL_SIZE_PROPERTY = "spring.datasource.hikari.maximum-pool-size"

@Configuration
class DatabasePoolConfig {
    companion object {
        // Static and lazy so that registering a post processor doesn't pull these beans in early
        @JvmStatic
        @Bean
        fun hikariPoolSizer(
            environment: Environment,
            jeluProperties: ObjectProvider<JeluProperties>,
        ): BeanPostProcessor = HikariPoolSizer(environment, jeluProperties)
    }
}

/**
 * Sizes the pool from jelu.database, because a yml placeholder can't pick a default based on
 * journalMode. Setting spring.datasource.hikari.maximum-pool-size directly still wins.
 */
class HikariPoolSizer(
    private val environment: Environment,
    private val jeluProperties: ObjectProvider<JeluProperties>,
) : BeanPostProcessor {
    override fun postProcessAfterInitialization(
        bean: Any,
        beanName: String,
    ): Any {
        if (bean !is HikariDataSource) {
            return bean
        }
        if (Binder.get(environment).bind(HIKARI_POOL_SIZE_PROPERTY, Int::class.javaObjectType).isBound) {
            logger.info { "Database pool size of ${bean.maximumPoolSize} taken from $HIKARI_POOL_SIZE_PROPERTY" }
            return bean
        }
        val database = jeluProperties.getObject().database
        bean.maximumPoolSize = database.poolSizeOrDefault()
        logger.info { "Database pool size of ${bean.maximumPoolSize} with journal mode ${database.journalMode}" }
        return bean
    }
}
