package io.github.bayang.jelu.config

import org.springframework.context.annotation.Bean
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

@EnableWebSecurity
class SecurityConfig(
    private val authenticationProvider: AuthenticationProvider?,
    private val properties: JeluProperties,
    private val userDetailsService: UserDetailsService,
    private val passwordEncoder: PasswordEncoder,
) {

    @Bean
    @Throws(Exception::class)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain? {
        http
            .cors { }
            .csrf { it.disable() }
            .logout { it ->
                it.logoutUrl("/api/v1/logout")
                    .invalidateHttpSession(true)
            }
            .authorizeRequests {
                it.antMatchers(
                    "/api/v1/token", "/api/v1/setup/status", "/api/v1/server-settings"
                ).permitAll()
                it.mvcMatchers(HttpMethod.POST, "/api/v1/users").hasAnyRole("ADMIN", "INITIAL_SETUP")
                it.mvcMatchers(HttpMethod.PUT, "/api/v1/users/**").hasAnyRole("USER")
                it.mvcMatchers(
                    HttpMethod.GET,
                    "/api/v1/users/me",
                ).hasRole("USER")
                it.antMatchers(
                    "/api/v1/users/**",
                ).hasRole("ADMIN")
                it.antMatchers(
                    "/api/v1/users",
                ).hasRole("ADMIN")
                it.mvcMatchers(
                    HttpMethod.POST,
                    "/api/v1/user-messages",
                ).hasRole("ADMIN")
                it.antMatchers(
                    "/api/**",
                ).hasRole("USER")
            }
            .httpBasic()
            .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
        if (properties.auth.ldap.enabled) {
            val dao = DaoAuthenticationProvider()
            dao.setUserDetailsService(userDetailsService)
            dao.setPasswordEncoder(passwordEncoder)
            http.authenticationManager(ProviderManager(authenticationProvider, dao))
        }
        return http.build()
    }
}
