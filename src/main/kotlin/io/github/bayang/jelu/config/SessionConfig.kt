package io.github.bayang.jelu.config

import com.github.gotson.spring.session.caffeine.CaffeineIndexedSessionRepository
import com.github.gotson.spring.session.caffeine.config.annotation.web.http.EnableCaffeineHttpSession
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.session.SessionRegistry
import org.springframework.session.FindByIndexNameSessionRepository
import org.springframework.session.config.SessionRepositoryCustomizer
import org.springframework.session.security.SpringSessionBackedSessionRegistry
import org.springframework.session.web.http.HeaderHttpSessionIdResolver
import org.springframework.session.web.http.HttpSessionIdResolver

@EnableCaffeineHttpSession
@Configuration
class SessionConfig {

    @Bean
    fun httpSessionIdResolver(): HttpSessionIdResolver {
        return HeaderHttpSessionIdResolver.xAuthToken()
    }

    @Bean
    fun sessionRegistry(sessionRepository: FindByIndexNameSessionRepository<*>): SessionRegistry =
        SpringSessionBackedSessionRegistry(sessionRepository)

    @Bean
    fun customizeSessionRepository(properties: JeluProperties) =
        SessionRepositoryCustomizer<CaffeineIndexedSessionRepository>() {
            it.setDefaultMaxInactiveInterval(properties.session.duration)
        }
}
