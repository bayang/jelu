package io.github.bayang.jelu.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.ldap.core.support.BaseLdapPathContextSource
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.ldap.DefaultSpringSecurityContextSource
import org.springframework.security.ldap.authentication.AbstractLdapAuthenticator
import org.springframework.security.ldap.authentication.BindAuthenticator
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper

@Configuration
@ConditionalOnProperty(name = ["jelu.auth.ldap.enabled"], havingValue = "true", matchIfMissing = false)
class LdapConfig(
    private val userDetailsContextMapper: UserDetailsContextMapper,
    private val properties: JeluProperties,
) {
    @Bean
    fun contextSource(): BaseLdapPathContextSource {
        val context: DefaultSpringSecurityContextSource = DefaultSpringSecurityContextSource(properties.auth.ldap.url)

        if (!properties.auth.ldap.userDn
                .isNullOrBlank()
        ) {
            context.setUserDn(properties.auth.ldap.userDn)
        }
        if (!properties.auth.ldap.password
                .isNullOrBlank()
        ) {
            context.setPassword(properties.auth.ldap.password)
        }
        return context
    }

    @Bean
    fun authenticationProvider(contextSource: BaseLdapPathContextSource): AuthenticationProvider {
        val authenticator: AbstractLdapAuthenticator = BindAuthenticator(contextSource)
        if (properties.auth.ldap.userDnPatterns
                .isNotEmpty()
        ) {
            authenticator.setUserDnPatterns(
                properties.auth.ldap.userDnPatterns
                    .toTypedArray(),
            )
        }
        if (!properties.auth.ldap.userSearchFilter
                .isNullOrBlank()
        ) {
            val userSearchBase =
                if (properties.auth.ldap.userSearchBase
                        .isNullOrBlank()
                ) {
                    ""
                } else {
                    properties.auth.ldap.userSearchBase
                }
            authenticator.setUserSearch(
                FilterBasedLdapUserSearch(userSearchBase, properties.auth.ldap.userSearchFilter, contextSource),
            )
        }
        authenticator.afterPropertiesSet()
        val provider: LdapAuthenticationProvider = LdapAuthenticationProvider(authenticator)
        provider.setUserDetailsContextMapper(userDetailsContextMapper)
        return provider
    }
}
