package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dto.*
import io.github.bayang.jelu.service.UserService
import mu.KotlinLogging
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.security.Principal
import java.util.*
import javax.validation.Valid

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api")
class UsersController(
    private val repository: UserService,
    private val properties: JeluProperties
    ) {

    @GetMapping(path = ["/users/me"])
    fun authenticatedUser(principal: Authentication): UserDto {
        if (principal != null) {
            when (principal.principal) {
                is DummyUser -> {
                    logger.debug { "dummy user $principal" }
                    return UserDto(
                        email = principal.name,
                        isAdmin = true,
                        id = null,
                        password = "****",
                        modificationDate = null,
                        creationDate = null
                    )
                }
                is JeluUser -> {
                    logger.debug { "jelu user $principal" }
                    return UserDto(
                        email = principal.name,
                        isAdmin = (principal.principal as JeluUser).user.isAdmin,
                        id = null,
                        password = "****",
                        modificationDate = null,
                        creationDate = null
                    )
                }
                else -> {
                    logger.debug { "other principal $principal" }
                    return UserDto(
                        email = principal.name,
                        isAdmin = principal.authorities.contains(SimpleGrantedAuthority(ROLE_ADMIN)),
                        id = null,
                        password = "****",
                        modificationDate = null,
                        creationDate = null
                    )
                }
            }
        }
        else {
            logger.debug { "no principal" }
            return UserDto(
                email = principal.name,
                isAdmin = (principal.principal as JeluUser).user.isAdmin,
                id= null,
                password = "****",
                modificationDate = null,
                creationDate = null)
        }
    }

    @GetMapping(path = ["/users"])
    fun users(@RequestParam(name = "q", required = false) searchTerm: String?): List<UserDto> = repository.findAll(searchTerm)

    @GetMapping(path = ["/users/{id}"])
    fun userById(@PathVariable("id") userId: UUID): UserDtoWithEvents = repository.findUserById(userId)

    @PostMapping(path = ["/users"])
    fun saveUser(@RequestBody @Valid user: CreateUserDto): UserDto {
        return repository.save(user)
    }

}