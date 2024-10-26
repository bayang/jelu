package io.github.bayang.jelu.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val authenticationProvider: AuthenticationProvider?,
    private val properties: JeluProperties,
    private val userDetailsService: UserDetailsService,
    private val passwordEncoder: PasswordEncoder,
    private val authHeaderFilter: AuthHeaderFilter?,
    private val userAgentWebAuthenticationDetailsSource: WebAuthenticationDetailsSource,
) {

    @Bean
    @Throws(Exception::class)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain? {
        http
            .cors { }
            .csrf { it.disable() }
            .logout {
                it.logoutUrl("/api/v1/logout")
                    .invalidateHttpSession(true)
            }
            .securityMatchers {
                // only apply security to those endpoints
                it.requestMatchers(
                    "/api/**",
                )
            }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/api/v1/token",
                    "/api/v1/setup/status",
                    "/api/v1/server-settings",
                    "/api/v1/reviews/**",
                ).permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/v1/reviews/**").permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/v1/books/**").permitAll()
                it.requestMatchers(HttpMethod.GET, "/api\\/v1\\/users\\/([a-zA-Z0-9-]*)\\/name$").permitAll()
                it.requestMatchers(HttpMethod.POST, "/api/v1/users").hasAnyRole("ADMIN", "INITIAL_SETUP")
                it.requestMatchers(HttpMethod.PUT, "/api/v1/users/**").hasAnyRole("USER")
                it.requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/users/me",
                ).hasRole("USER")
                it.requestMatchers(
                    "/api/v1/users/**",
                ).hasRole("USER")
                it.requestMatchers(
                    "/api/v1/users",
                ).hasRole("USER")
                it.requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/user-messages",
                ).hasRole("ADMIN")
                it.requestMatchers(
                    "/api/**",
                ).hasRole("USER")
            }
            .httpBasic {
                it.authenticationDetailsSource(userAgentWebAuthenticationDetailsSource)
            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            }
        if (properties.auth.ldap.enabled) {
            val dao = DaoAuthenticationProvider()
            dao.setUserDetailsService(userDetailsService)
            dao.setPasswordEncoder(passwordEncoder)
            http.authenticationManager(ProviderManager(authenticationProvider, dao))
        }
        if (properties.auth.proxy.enabled) {
            http.addFilterBefore(authHeaderFilter, UsernamePasswordAuthenticationFilter::class.java)
        }
        return http.build()
    }
}
