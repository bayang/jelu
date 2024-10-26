package io.github.bayang.jelu.config

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.BeanClassLoaderAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.support.GenericConversionService
import org.springframework.core.serializer.Deserializer
import org.springframework.core.serializer.Serializer
import org.springframework.core.serializer.support.DeserializingConverter
import org.springframework.core.serializer.support.SerializingConverter
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.jackson2.SecurityJackson2Modules
import org.springframework.session.FindByIndexNameSessionRepository
import org.springframework.session.config.SessionRepositoryCustomizer
import org.springframework.session.jdbc.JdbcIndexedSessionRepository
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession
import org.springframework.session.security.SpringSessionBackedSessionRegistry
import org.springframework.session.web.http.HeaderHttpSessionIdResolver
import org.springframework.session.web.http.HttpSessionIdResolver
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.Duration

@EnableJdbcHttpSession
@Configuration
class SessionConfig : BeanClassLoaderAware {

    val CREATE_SESSION_ATTRIBUTE_QUERY: String =
        "INSERT INTO %TABLE_NAME%_ATTRIBUTES (SESSION_PRIMARY_ID, ATTRIBUTE_NAME, ATTRIBUTE_BYTES)\nVALUES (?, ?, json(?))\n"
    val UPDATE_SESSION_ATTRIBUTE_QUERY: String =
        "UPDATE %TABLE_NAME%_ATTRIBUTES\nSET ATTRIBUTE_BYTES = json(?)\nWHERE SESSION_PRIMARY_ID = ?\nAND ATTRIBUTE_NAME = ?\n"
    val DELETE_SESSIONS_BY_EXPIRY_TIME_QUERY: String =
        "DELETE FROM %TABLE_NAME% WHERE PRIMARY_ID IN (SELECT SESSION_PRIMARY_ID FROM %TABLE_NAME%_ATTRIBUTES WHERE ATTRIBUTE_NAME = 'org.springframework.session.security.SpringSessionBackedSessionInformation.EXPIRED' AND ATTRIBUTE_BYTES = 'true') OR EXPIRY_TIME < ?\n"

    @Bean
    fun httpSessionIdResolver(): HttpSessionIdResolver {
        return HeaderHttpSessionIdResolver.xAuthToken()
    }

    @Bean
    fun sessionRegistry(sessionRepository: FindByIndexNameSessionRepository<*>): SessionRegistry =
        SpringSessionBackedSessionRegistry(sessionRepository)

    @Bean
    fun customizeSessionRepository(properties: JeluProperties) =
        SessionRepositoryCustomizer<JdbcIndexedSessionRepository>() {
            it.setDefaultMaxInactiveInterval(Duration.ofSeconds(properties.session.duration))
            it.setCreateSessionAttributeQuery(CREATE_SESSION_ATTRIBUTE_QUERY)
            it.setUpdateSessionAttributeQuery(UPDATE_SESSION_ATTRIBUTE_QUERY)
            it.setDeleteSessionsByExpiryTimeQuery(DELETE_SESSIONS_BY_EXPIRY_TIME_QUERY)
        }

    // https://github.com/spring-projects/spring-session/issues/1011#issuecomment-919639470
    // @Bean("springSessionTransactionOperations")
    // fun springSessionTransactionOperations(): TransactionOperations {
    //     return TransactionOperations.withoutTransaction()
    // }

    private var classLoader: ClassLoader? = null

    @Bean("springSessionConversionService")
    fun springSessionConversionService(objectMapper: ObjectMapper): GenericConversionService {
        val copy = objectMapper.copy()
        // https://docs.spring.io/spring-session/reference/configuration/jdbc.html#session-attributes-as-json
        // Register Spring Security Jackson Modules
        copy.registerModules(SecurityJackson2Modules.getModules(this.classLoader))
        copy.disable(MapperFeature.USE_GETTERS_AS_SETTERS) // mandatory to deserialize setterless authorities on user
        copy.addMixIn(UserAgentWebAuthenticationDetails::class.java, UserAgentWebAuthenticationDetailsMixin::class.java)
        val converter = GenericConversionService()
        converter.addConverter(Any::class.java, ByteArray::class.java, SerializingConverter(JsonSerializer(copy)))
        converter.addConverter(ByteArray::class.java, Any::class.java, DeserializingConverter(JsonDeserializer(copy)))
        return converter
    }

    override fun setBeanClassLoader(classLoader: ClassLoader) {
        this.classLoader = classLoader
    }

    class JsonSerializer internal constructor(private val objectMapper: ObjectMapper) :
        Serializer<Any?> {
        @Throws(IOException::class)
        override fun serialize(`object`: Any, outputStream: OutputStream) {
            objectMapper.writeValue(outputStream, `object`)
        }
    }

    class JsonDeserializer internal constructor(private val objectMapper: ObjectMapper) :
        Deserializer<Any?> {
        @Throws(IOException::class)
        override fun deserialize(inputStream: InputStream): Any {
            return objectMapper.readValue(inputStream, Any::class.java)
        }
    }
}
