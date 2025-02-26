package io.github.bayang.jelu.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.github.bayang.jelu.errors.JeluException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User

const val ROLE_PREFIX: String = "ROLE_"

const val ROLE_USER: String = ROLE_PREFIX + "USER"

const val ROLE_ADMIN: String = ROLE_PREFIX + "ADMIN"

const val ROLE_INITIAL_SETUP: String = ROLE_PREFIX + "INITIAL_SETUP"

fun assertIsJeluUser(target: Any) {
    if (target !is JeluUser) {
        throw JeluException("Logged in user/provided credentials cannot access")
    }
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
)
@JsonSubTypes(
    Type(value = JeluUser::class, name = "jeluUser"),
)
class JeluUser(
    val user: UserDto,
    val oAuth2User: OAuth2User? = null,
    val oidcUser: OidcUser? = null,
    val initialSetup: Boolean = false,
) : UserDetails, OAuth2User, OidcUser {
    override fun getName(): String {
        return user.login
    }

    override fun getAttributes(): MutableMap<String, Any> {
        return oAuth2User?.attributes ?: mutableMapOf()
    }

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        val basicRoles: MutableCollection<GrantedAuthority> = mutableSetOf()
        if (initialSetup) {
            basicRoles.add(SimpleGrantedAuthority(ROLE_INITIAL_SETUP))
        } else {
            basicRoles.add(SimpleGrantedAuthority(ROLE_USER))
            if (user.isAdmin) {
                basicRoles.add(SimpleGrantedAuthority(ROLE_ADMIN))
            }
        }
        return basicRoles
    }

    override fun getClaims(): MutableMap<String, Any> = oidcUser?.claims ?: mutableMapOf()

    override fun getUserInfo(): OidcUserInfo? = oidcUser?.userInfo

    override fun getIdToken(): OidcIdToken? = oidcUser?.idToken

    override fun getPassword(): String {
        return user.password.orEmpty()
    }

    override fun getUsername(): String = user.login

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true
}
