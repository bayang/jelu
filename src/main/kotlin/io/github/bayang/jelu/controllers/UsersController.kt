package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dto.CreateUserDto
import io.github.bayang.jelu.dto.UserDto
import io.github.bayang.jelu.dto.UserDtoWithEvents
import io.github.bayang.jelu.service.UserService
import mu.KotlinLogging
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.validation.Valid

private val logger = KotlinLogging.logger {}

@RestController
class UsersController(
    private val repository: UserService,
    private val properties: JeluProperties
    ) {

    @GetMapping(path = ["/users"])
    fun users(@RequestParam(name = "q", required = false) searchTerm: String?): List<UserDto> = repository.findAll(searchTerm)

    @GetMapping(path = ["/users/{id}"])
    fun userById(@PathVariable("id") userId: UUID): UserDtoWithEvents = repository.findUserById(userId)

    @PostMapping(path = ["/users"])
    fun saveUser(@RequestBody @Valid user: CreateUserDto): UserDto {
        return repository.save(user)
    }

}