package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.config.UserAgentWebAuthenticationDetails
import io.github.bayang.jelu.dto.AuthenticationDto
import io.github.bayang.jelu.dto.CreateUserDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.dto.LoginHistoryInfoDto
import io.github.bayang.jelu.dto.ROLE_ADMIN
import io.github.bayang.jelu.dto.UpdateUserDto
import io.github.bayang.jelu.dto.UserDto
import io.github.bayang.jelu.errors.JeluAuthenticationException
import io.github.bayang.jelu.errors.JeluValidationException
import io.github.bayang.jelu.service.UserService
import io.github.bayang.jelu.utils.stringFormat
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.servlet.http.HttpSession
import jakarta.validation.Valid
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.core.session.SessionRegistry
import org.springframework.session.FindByIndexNameSessionRepository
import org.springframework.session.Session
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

private val logger = KotlinLogging.logger {}

private const val SPRING_SECURITY_CONTEXT = "SPRING_SECURITY_CONTEXT"

@RestController
@RequestMapping("/api/v1")
class UsersController(
    private val repository: UserService,
    private val sessionsRepo: FindByIndexNameSessionRepository<out Session>,
    private val sessionRegistry: SessionRegistry,
) {

    @Operation(description = "get the current session token that the caller should provide in the X-Auth-Token header")
    @GetMapping(path = ["/token"])
    fun getToken(session: HttpSession) = mapOf<String, String>("token" to session.id)

    @Operation(description = "tells if the initial setup has been done (first user has been created)")
    @GetMapping(path = ["/setup/status"])
    fun setupStatus(session: HttpSession) = mapOf<String, Boolean>("isInitialSetup" to repository.isInitialSetup())

    @GetMapping(path = ["/users/me"])
    fun authenticatedUser(principal: Authentication, session: HttpSession): AuthenticationDto {
        when (principal.principal) {
            is JeluUser -> {
                logger.trace { "jelu user $principal" }
                logger.trace { "session ${session.id}" }
                return AuthenticationDto(
                    UserDto(
                        login = principal.name,
                        isAdmin = (principal.principal as JeluUser).user.isAdmin,
                        id = (principal.principal as JeluUser).user.id,
                        password = "****",
                        modificationDate = null,
                        creationDate = null,
                        provider = (principal.principal as JeluUser).user.provider,
                    ),
                    token = session.id,
                )
            }
            else -> {
                logger.trace { "other principal $principal" }
                return AuthenticationDto(
                    UserDto(
                        login = principal.name,
                        isAdmin = principal.authorities.contains(SimpleGrantedAuthority(ROLE_ADMIN)),
                        id = null,
                        password = "****",
                        modificationDate = null,
                        creationDate = null,
                    ),
                    token = session.id,
                )
            }
        }
    }

    @GetMapping(path = ["/users"])
    fun users(@RequestParam(name = "q", required = false) searchTerm: String?): List<UserDto> = repository.findAll(searchTerm)

    @GetMapping(path = ["/users/{id}"])
    fun userById(@PathVariable("id") userId: UUID): UserDto = repository.findUserById(userId)

    @GetMapping(path = ["/users/{id}/name"])
    fun usernameById(@PathVariable("id") userId: UUID) = mapOf<String, String>("username" to repository.findUserById(userId).login)

    @PutMapping(path = ["/users/{id}"])
    fun updateUser(
        @PathVariable("id")
        userId: UUID,
        @RequestBody
        @Valid
        user: UpdateUserDto,
        principal: Authentication,
        session: HttpSession,
    ): UserDto {
        if (principal.principal is JeluUser) {
            if ((principal.principal as JeluUser).user.isAdmin || (principal.principal as JeluUser).user.id == userId) {
                // only admin user can remove or add admin rights
                val cleanedUpdateUserDto: UpdateUserDto = if ((principal.principal as JeluUser).user.isAdmin) {
                    user
                } else {
                    user.copy(isAdmin = null)
                }
                val res = repository.updateUser(userId, cleanedUpdateUserDto)
                sessionRegistry.getAllSessions(res.login, false).forEach {
                    it.expireNow()
                }
                return res
            }
        }
        throw JeluAuthenticationException("principal ${principal.name} not allowed to edit user $userId")
    }

    @ApiResponse(responseCode = "204", description = "Deleted the user")
    @DeleteMapping(path = ["/users/{id}"])
    fun deleteUser(
        @PathVariable("id")
        userId: UUID,
        principal: Authentication,
    ): ResponseEntity<Unit> {
        if (principal.principal is JeluUser && (principal.principal as JeluUser).user.isAdmin) {
            if ((principal.principal as JeluUser).user.id == userId) {
                // prevent admin from trying to delete itself
                throw JeluValidationException("Principal ${principal.name} is deleting itself")
            }
            repository.deleteUser(userId)
            return ResponseEntity.noContent().build()
        }
        throw JeluAuthenticationException("Only user with admin rights can delete users")
    }

    @PostMapping(path = ["/users"])
    fun saveUser(
        @RequestBody @Valid
        user: CreateUserDto,
    ): UserDto {
        return repository.save(user)
    }

    @GetMapping(path = ["/users/history"])
    fun getUserHistory(principal: Authentication): MutableList<LoginHistoryInfoDto> {
        val history = mutableListOf<LoginHistoryInfoDto>()
        if (principal.principal is JeluUser) {
            val res: MutableMap<String, out Session> = sessionsRepo.findByPrincipalName(principal.name)
            res.forEach {
                val ctx = it.value.getAttribute<SecurityContextImpl>(SPRING_SECURITY_CONTEXT)
                history.add(mapToHistoryDto(it, ctx))
            }
        }
        return history
    }

    private fun mapToHistoryDto(entry: Map.Entry<String, Session>, ctx: SecurityContextImpl?): LoginHistoryInfoDto {
        val details = ctx?.authentication?.details as UserAgentWebAuthenticationDetails
        val jeluUser = ctx.authentication.principal as JeluUser
        return LoginHistoryInfoDto(
            ip = details.remoteAddress.orEmpty(),
            userAgent = details.userAgent,
            source = jeluUser.user.provider.name,
            date = stringFormat(entry.value.lastAccessedTime),
        )
    }
}
