package io.github.bayang.jelu.config

import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

@EnableWebSecurity
class SecurityConfig: WebSecurityConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {
        http
            .csrf{ it.disable() }
            .authorizeRequests {
                it.mvcMatchers(HttpMethod.POST, "/api/users").hasAnyRole("ADMIN", "INITIAL_SETUP")
                it.antMatchers(
                    "/api/users**",
                ).hasRole("ADMIN")
                it.antMatchers(
                    "/api/**",
                ).hasRole("USER")
            }
            .httpBasic()
    }
}