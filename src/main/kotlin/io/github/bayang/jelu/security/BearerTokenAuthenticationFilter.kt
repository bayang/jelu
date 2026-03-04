package io.github.bayang.jelu.security

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.service.ApiTokenService
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

private val logger = KotlinLogging.logger {}

const val BEARER_PREFIX = "Bearer "

/**
 * Filter that processes Bearer token authentication for API tokens.
 * This filter:
 * 1. Extracts the Bearer token from the Authorization header
 * 2. Validates the token against the database
 * 3. Creates an authentication with the token's scopes as authorities
 * 4. Updates last used timestamp asynchronously
 */
@Component
class BearerTokenAuthenticationFilter(
    private val apiTokenService: ApiTokenService,
) : OncePerRequestFilter() {
    // Use request-attribute based context repository to avoid session storage
    private val securityContextRepository = RequestAttributeSecurityContextRepository()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        // Only process if no authentication is already set
        if (SecurityContextHolder.getContext().authentication != null) {
            filterChain.doFilter(request, response)
            return
        }

        val authHeader = request.getHeader(HttpHeaders.AUTHORIZATION)

        // Check if this is a Bearer token request
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response)
            return
        }

        val rawToken = authHeader.removePrefix(BEARER_PREFIX).trim()

        if (rawToken.isBlank()) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            val validatedToken = apiTokenService.validateToken(rawToken)

            if (validatedToken != null) {
                // Build authorities from scopes + base USER role
                val authorities = buildAuthorities(validatedToken.scopes)

                // Create JeluUser from the token's user
                val jeluUser = JeluUser(validatedToken.user)

                // Create authentication
                val authentication =
                    ApiTokenAuthentication(
                        principal = jeluUser,
                        credentials = null,
                        authorities = authorities,
                        tokenId = validatedToken.id,
                        tokenName = validatedToken.name,
                        scopes = validatedToken.scopes,
                    )

                // Create a new security context and set it (don't persist to session)
                val securityContext = SecurityContextHolder.createEmptyContext()
                securityContext.authentication = authentication
                SecurityContextHolder.setContext(securityContext)

                // Save context to request attributes only (not to session)
                securityContextRepository.saveContext(securityContext, request, response)

                logger.debug { "Authenticated via API token '${validatedToken.name}' for user '${jeluUser.username}'" }

                // Update last used asynchronously
                apiTokenService.updateLastUsedAsync(validatedToken.id)

                // Check scope access for this request
                val path = request.requestURI
                val method = request.method

                if (!ScopePathMatcher.hasRequiredScope(path, method, validatedToken.scopes)) {
                    logger.debug { "Token '${validatedToken.name}' lacks required scope for $method $path" }
                    response.sendError(
                        HttpServletResponse.SC_FORBIDDEN,
                        "Insufficient scope for this operation",
                    )
                    return
                }

                // Token is valid and has required scope, continue with filter chain
                filterChain.doFilter(request, response)
                return
            } else {
                // Bearer token was provided but is invalid/inactive/expired
                logger.debug { "Invalid or expired API token" }
                response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired API token",
                )
                return
            }
        } catch (e: Exception) {
            logger.warn { "Error validating API token: ${e.message}" }
            response.sendError(
                HttpServletResponse.SC_UNAUTHORIZED,
                "Error validating API token",
            )
            return
        }
    }

    private fun buildAuthorities(scopes: List<String>): List<GrantedAuthority> {
        val authorities = mutableListOf<GrantedAuthority>()

        // Add base USER role
        authorities.add(SimpleGrantedAuthority("ROLE_USER"))

        // Add scope-based authorities
        authorities.addAll(TokenScope.toGrantedAuthorities(scopes))

        return authorities
    }
}

/**
 * Custom authentication class for API token authenticated requests.
 * Extends UsernamePasswordAuthenticationToken but includes token-specific metadata.
 */
class ApiTokenAuthentication(
    principal: JeluUser,
    credentials: Any?,
    authorities: Collection<GrantedAuthority>,
    val tokenId: UUID,
    val tokenName: String,
    val scopes: List<String>,
) : UsernamePasswordAuthenticationToken(principal, credentials, authorities) {
    /**
     * Check if this authentication has a specific scope
     */
    fun hasScope(scope: String): Boolean = scopes.contains(scope)

    /**
     * Check if this authentication has any of the specified scopes
     */
    fun hasAnyScope(vararg requiredScopes: String): Boolean = requiredScopes.any { it in scopes }

    /**
     * Check if this authentication has all of the specified scopes
     */
    fun hasAllScopes(vararg requiredScopes: String): Boolean = requiredScopes.all { it in scopes }
}

/**
 * Jackson mixin for ApiTokenAuthentication to support session serialization.
 * This is needed when sessions contain ApiTokenAuthentication objects that need
 * to be serialized/deserialized from the session store.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.ANY,
)
abstract class ApiTokenAuthenticationMixin
    @JsonCreator
    constructor(
        @JsonProperty("principal") principal: JeluUser,
        @JsonProperty("credentials") credentials: Any?,
        @JsonProperty("authorities") authorities: Collection<GrantedAuthority>,
        @JsonProperty("tokenId") tokenId: UUID,
        @JsonProperty("tokenName") tokenName: String,
        @JsonProperty("scopes") scopes: List<String>,
    )
