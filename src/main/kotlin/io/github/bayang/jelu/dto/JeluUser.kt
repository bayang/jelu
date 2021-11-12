package io.github.bayang.jelu.dto

import io.github.bayang.jelu.dao.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

const val ROLE_PREFIX:String = "ROLE_";

const val ROLE_USER:String = ROLE_PREFIX+ "USER";

const val ROLE_ADMIN:String = ROLE_PREFIX+ "ADMIN";

const val ROLE_INITIAL_SETUP:String = ROLE_PREFIX+ "INITIAL_SETUP";

class JeluUser(val user: User): UserDetails {

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        val basicRoles: MutableCollection<GrantedAuthority> = mutableSetOf(SimpleGrantedAuthority(ROLE_USER))
        if (user.isAdmin) {
            basicRoles.add(SimpleGrantedAuthority(ROLE_ADMIN))
        }
        return basicRoles
    }

    override fun getPassword(): String = user.password

    override fun getUsername(): String = user.email

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true
}