package io.github.bayang.jelu.security.oauth2

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dao.Provider
import io.github.bayang.jelu.dto.CreateUserDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.service.UserService
import mu.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User

private val logger = KotlinLogging.logger {}

/**
 * code comes from Gotson's Komga app https://github.com/gotson/komga
 */
@Configuration
class AppOAuth2UserServiceConfiguration(
    private val userRepository: UserService,
    private val jeluProperties: JeluProperties,
) {
    @Bean
    fun oauth2UserService(): OAuth2UserService<OAuth2UserRequest, OAuth2User> {
        val defaultDelegate = DefaultOAuth2UserService()
        val githubDelegate = GithubOAuth2UserService()

        return OAuth2UserService { userRequest: OAuth2UserRequest ->
            val delegate =
                when (userRequest.clientRegistration.registrationId.lowercase()) {
                    "github" -> githubDelegate
                    else -> defaultDelegate
                }

            val oAuth2User = delegate.loadUser(userRequest)

            val email =
                oAuth2User.getAttribute<String>("email")
                    ?: throw OAuth2AuthenticationException("ERR_1024")

            val existingUser = userRepository.findByLogin(email)
            if (existingUser.isEmpty()) {
                tryCreateNewUser(email)
            } else {
                JeluUser(existingUser.first(), oAuth2User = oAuth2User)
            }
        }
    }

    @Bean
    fun oidcUserService(): OAuth2UserService<OidcUserRequest, OidcUser> {
        val delegate = OidcUserService()
        return OAuth2UserService { userRequest: OidcUserRequest ->
            val oidcUser = delegate.loadUser(userRequest)

            if (oidcUser.email == null) throw OAuth2AuthenticationException("ERR_1028")
            if (jeluProperties.auth.oidcEmailVerification && oidcUser.emailVerified == null) throw OAuth2AuthenticationException("ERR_1027")
            if (jeluProperties.auth.oidcEmailVerification && oidcUser.emailVerified == false) throw OAuth2AuthenticationException("ERR_1026")

            val existingUser = userRepository.findByLogin(oidcUser.email)
            if (existingUser.isEmpty()) {
                tryCreateNewUser(oidcUser.email)
            } else {
                JeluUser(existingUser.first(), oidcUser = oidcUser)
            }
        }
    }

    private fun tryCreateNewUser(email: String) =
        if (jeluProperties.auth.oauth2AccountCreation) {
            logger.info { "Creating new user from OAuth2 login: $email" }
            val saved = userRepository.save(CreateUserDto(login = email, password = RandomStringUtils.secure().nextAlphanumeric(12), isAdmin = false, Provider.OIDC))
            JeluUser(saved)
        } else {
            throw OAuth2AuthenticationException("ERR_1025")
        }
}
