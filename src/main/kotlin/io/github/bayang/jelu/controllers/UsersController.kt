package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dto.*
import io.github.bayang.jelu.service.UserService
import mu.KotlinLogging
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.HttpSession
import javax.validation.Valid

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api")
class UsersController(
    private val repository: UserService,
    private val properties: JeluProperties
) {

    @GetMapping(path = ["/token"])
    fun getToken(session: HttpSession) = mapOf<String, String>("token" to session.id)

    @GetMapping(path = ["/setup/status"])
    fun setupStatus(session: HttpSession) = mapOf<String, Boolean>("isInitialSetup" to repository.isInitialSetup())

    @GetMapping(path = ["/users/me"])
    fun authenticatedUser(principal: Authentication, session: HttpSession): AuthenticationDto {
        if (principal != null) {
            when (principal.principal) {
                is DummyUser -> {
                    logger.debug { "dummy user $principal" }
                    return AuthenticationDto(
                        UserDto(
                            login = principal.name,
                            isAdmin = true,
                            id = null,
                            password = "****",
                            modificationDate = null,
                            creationDate = null
                        ),
                        token = session.id
                    )
                }
                is JeluUser -> {
                    logger.debug { "jelu user $principal" }
                    logger.info { "session ${session.id}" }
                    return AuthenticationDto(
                        UserDto(
                            login = principal.name,
                            isAdmin = (principal.principal as JeluUser).user.isAdmin,
                            id = (principal.principal as JeluUser).user.id.value,
                            password = "****",
                            modificationDate = null,
                            creationDate = null
                        ),
                        token = session.id
                    )
                }
                else -> {
                    logger.debug { "other principal $principal" }
                    return AuthenticationDto(
                        UserDto(
                            login = principal.name,
                            isAdmin = principal.authorities.contains(SimpleGrantedAuthority(ROLE_ADMIN)),
                            id = null,
                            password = "****",
                            modificationDate = null,
                            creationDate = null
                        ),
                        token = session.id
                    )
                }
            }
        } else {
            logger.debug { "no principal" }
            return AuthenticationDto(
                UserDto(
                    login = principal.name,
                    isAdmin = (principal.principal as JeluUser).user.isAdmin,
                    id = null,
                    password = "****",
                    modificationDate = null,
                    creationDate = null
                ),
                token = session.id
            )
        }
    }

    @GetMapping(path = ["/users"])
    fun users(@RequestParam(name = "q", required = false) searchTerm: String?): List<UserDto> = repository.findAll(searchTerm)

    @GetMapping(path = ["/users/{id}"])
    fun userById(@PathVariable("id") userId: UUID): UserDto = repository.findUserById(userId)

    @PostMapping(path = ["/users"])
    fun saveUser(@RequestBody @Valid user: CreateUserDto): UserDto {
        return repository.save(user)
    }
}
