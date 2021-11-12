package io.github.bayang.jelu.dto

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class DummyUser(private val startupPassword: String): UserDetails {
    override fun getAuthorities(): MutableCollection<out GrantedAuthority> = mutableSetOf(
        SimpleGrantedAuthority(ROLE_INITIAL_SETUP)
    )

    override fun getPassword(): String = startupPassword

    override fun getUsername(): String = "setup"

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true
}