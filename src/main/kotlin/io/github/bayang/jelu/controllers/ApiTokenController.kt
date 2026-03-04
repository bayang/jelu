package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.dto.AdminApiTokenDto
import io.github.bayang.jelu.dto.ApiTokenCreatedDto
import io.github.bayang.jelu.dto.ApiTokenDto
import io.github.bayang.jelu.dto.CreateApiTokenDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.dto.TokenScopeDto
import io.github.bayang.jelu.dto.UpdateApiTokenDto
import io.github.bayang.jelu.errors.JeluNotFoundException
import io.github.bayang.jelu.service.ApiTokenService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
@Tag(name = "API Tokens", description = "Manage API tokens for programmatic access")
class ApiTokenController(
    private val apiTokenService: ApiTokenService,
) {
    @Operation(summary = "List user's API tokens")
    @GetMapping(path = ["/api-tokens"])
    fun listTokens(principal: Authentication): List<ApiTokenDto> {
        val user = (principal.principal as JeluUser).user
        return apiTokenService.findByUserId(user.id!!)
    }

    @Operation(summary = "Create a new API token")
    @ApiResponse(responseCode = "201", description = "Token created successfully")
    @PostMapping(path = ["/api-tokens"])
    fun createToken(
        @RequestBody @Valid createDto: CreateApiTokenDto,
        principal: Authentication,
    ): ResponseEntity<ApiTokenCreatedDto> {
        val user = (principal.principal as JeluUser).user
        val createdToken = apiTokenService.createToken(createDto, user.id!!)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdToken)
    }

    @Operation(summary = "Get a specific API token")
    @GetMapping(path = ["/api-tokens/{id}"])
    fun getToken(
        @PathVariable("id") tokenId: UUID,
        principal: Authentication,
    ): ApiTokenDto {
        val user = (principal.principal as JeluUser).user
        return apiTokenService.findByIdAndUserId(tokenId, user.id!!)
            ?: throw JeluNotFoundException("Token not found")
    }

    @Operation(summary = "Update an API token")
    @PutMapping(path = ["/api-tokens/{id}"])
    fun updateToken(
        @PathVariable("id") tokenId: UUID,
        @RequestBody @Valid updateDto: UpdateApiTokenDto,
        principal: Authentication,
    ): ApiTokenDto {
        val user = (principal.principal as JeluUser).user
        return apiTokenService.updateToken(tokenId, user.id!!, updateDto)
            ?: throw JeluNotFoundException("Token not found")
    }

    @Operation(summary = "Revoke an API token")
    @ApiResponse(responseCode = "204", description = "Token revoked successfully")
    @DeleteMapping(path = ["/api-tokens/{id}"])
    fun revokeToken(
        @PathVariable("id") tokenId: UUID,
        principal: Authentication,
    ): ResponseEntity<Unit> {
        val user = (principal.principal as JeluUser).user
        val deleted = apiTokenService.revokeToken(tokenId, user.id!!)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            throw JeluNotFoundException("Token not found")
        }
    }

    @Operation(summary = "List available scopes")
    @GetMapping(path = ["/api-tokens/scopes"])
    fun listScopes(): List<TokenScopeDto> = TokenScopeDto.allScopes()

    /*
     * Admin endpoints - Are currently not instrumented in the frontend UI, but are preserved here
     * for future expansion to allow admins to manage all user's API tokens for their instance
     */

    @Operation(summary = "Admin: List all API tokens")
    @GetMapping(path = ["/admin/api-tokens"])
    fun adminListAllTokens(principal: Authentication): List<AdminApiTokenDto> {
        val user = (principal.principal as JeluUser).user
        if (!user.isAdmin) {
            throw org.springframework.security.access
                .AccessDeniedException("Admin access required")
        }
        return apiTokenService.findAll()
    }

    @Operation(summary = "Admin: Revoke any API token")
    @ApiResponse(responseCode = "204", description = "Token revoked successfully")
    @DeleteMapping(path = ["/admin/api-tokens/{id}"])
    fun adminRevokeToken(
        @PathVariable("id") tokenId: UUID,
        principal: Authentication,
    ): ResponseEntity<Unit> {
        val user = (principal.principal as JeluUser).user
        if (!user.isAdmin) {
            throw org.springframework.security.access
                .AccessDeniedException("Admin access required")
        }
        val deleted = apiTokenService.revokeToken(tokenId, user.id!!, isAdmin = true)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            throw JeluNotFoundException("Token not found")
        }
    }
}
