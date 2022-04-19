package io.github.bayang.jelu.config

import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy

@EnableWebSecurity
class SecurityConfig : WebSecurityConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {
        http
            .cors { }
            .csrf { it.disable() }
            .logout { it ->
                it.logoutUrl("/api/v1/logout")
                    .invalidateHttpSession(true)
            }
            .authorizeRequests {
                it.antMatchers(
                    "/api/v1/token", "/api/v1/setup/status"
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
                it.antMatchers(
                    "/api/**",
                ).hasRole("USER")
            }
            .httpBasic()
            .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
    }
}
