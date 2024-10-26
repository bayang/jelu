package io.github.bayang.jelu.config

import io.github.bayang.jelu.dao.Provider
import io.github.bayang.jelu.dao.UserRepository
import io.github.bayang.jelu.dto.CreateUserDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.dto.UpdateUserDto
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.ldap.core.DirContextAdapter
import org.springframework.ldap.core.DirContextOperations
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import javax.naming.directory.Attributes

private val logger = KotlinLogging.logger {}

@Component
@ConditionalOnProperty(name = ["jelu.auth.ldap.enabled"], havingValue = "true", matchIfMissing = false)
class JeluLdapUserDetailsContextMapper(
    private val userRepository: UserRepository,
) : UserDetailsContextMapper {

    @Transactional
    override fun mapUserFromContext(
        ctx: DirContextOperations?,
        username: String?,
        authorities: MutableCollection<out GrantedAuthority>?,
    ): UserDetails {
        dumpAttributesForDebug(ctx?.attributes)
        val isAdmin = findAdminMembership(ctx?.attributes)
        val res = userRepository.findByLoginAndProvider(username!!, Provider.LDAP)
        if (res.empty()) {
            val saved = userRepository.save(CreateUserDto(login = username, password = "ldap", isAdmin = isAdmin, Provider.LDAP))
            return JeluUser(saved.toUserDto())
        }
        var user = res.first()
        if (user.isAdmin != isAdmin) {
            user = userRepository.updateUser(user.id.value, UpdateUserDto(password = "ldap", isAdmin = isAdmin, provider = null))
        }
        return JeluUser(user.toUserDto())
    }

    private fun findAdminMembership(attributes: Attributes?): Boolean {
        var isAdmin = false
        if (attributes != null) {
            val all = attributes.all
            while (all.hasMoreElements()) {
                val att = all.next()
                if (att != null) {
                    if (att.id.equals("memberOf")) {
                        var values = ""
                        if (att.all != null) {
                            values = att.all.toList().joinToString()
                        }
                        if (!values.isNullOrBlank() && values.contains("jelu-admin")) {
                            isAdmin = true
                        }
                    }
                }
            }
        }
        return isAdmin
    }

    override fun mapUserToContext(user: UserDetails?, ctx: DirContextAdapter?) {
        TODO("Not yet implemented")
    }

    fun dumpAttributesForDebug(attributes: Attributes?) {
        logger.trace { "ldap attributes : " }
        if (attributes != null) {
            val all = attributes.all
            while (all.hasMoreElements()) {
                val att = all.next()
                if (att != null) {
                    var values = ""
                    if (att.all != null) {
                        values = att.all.toList().joinToString()
                    }
                    logger.trace { "Attribute: ${att.id} -> $values" }
                }
            }
        }
    }
}
