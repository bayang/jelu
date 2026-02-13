package io.github.bayang.jelu.config

import io.github.bayang.jelu.security.BearerTokenAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val authenticationProvider: AuthenticationProvider?,
    private val properties: JeluProperties,
    private val userDetailsService: UserDetailsService,
    private val passwordEncoder: PasswordEncoder,
    private val authHeaderFilter: AuthHeaderFilter?,
    private val bearerTokenAuthenticationFilter: BearerTokenAuthenticationFilter,
    private val userAgentWebAuthenticationDetailsSource: WebAuthenticationDetailsSource,
    private val oauth2UserService: OAuth2UserService<OAuth2UserRequest, OAuth2User>,
    private val oidcUserService: OAuth2UserService<OidcUserRequest, OidcUser>,
    clientRegistrationRepository: InMemoryClientRegistrationRepository?,
) {
    private val oauth2Enabled = clientRegistrationRepository != null

    @Bean
    @Throws(Exception::class)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain? {
        http
            .cors { }
            .csrf { it.disable() }
            .logout {
                it
                    .logoutUrl("/api/v1/logout")
                    .invalidateHttpSession(true)
            }.securityMatchers {
                // only apply security to those endpoints
                it.requestMatchers(
                    "/api/**",
                    "/oauth2/authorization/**",
                    "/login/oauth2/code/**",
                )
            }.authorizeHttpRequests {
                it
                    .requestMatchers(
                        "/api/v1/token",
                        "/api/v1/setup/status",
                        "/api/v1/server-settings",
                        "/api/v1/reviews/**",
                        "/api/v1/oauth2/providers",
                        "/api/v1/username/**",
                        "/api/v1/custom-lists/remove",
                        "/api/v1/api-tokens/scopes",
                    ).permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/v1/reviews/**").permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/v1/reviews").permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/v1/custom-lists/**").permitAll()
                // it.requestMatchers(HttpMethod.POST, "/api/v1/custom-lists/remove").permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/v1/books/**").permitAll()
                it.requestMatchers(HttpMethod.POST, "/api/v1/users").hasAnyRole("ADMIN", "INITIAL_SETUP")
                it.requestMatchers(HttpMethod.PUT, "/api/v1/users/**").hasAnyRole("USER")
                it
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/users/me",
                    ).hasRole("USER")
                it
                    .requestMatchers(
                        "/api/v1/users/**",
                    ).hasRole("USER")
                it
                    .requestMatchers(
                        "/api/v1/users",
                    ).hasRole("USER")
                it
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/user-messages",
                    ).hasRole("ADMIN")
                it
                    .requestMatchers(
                        "/api/**",
                    ).hasRole("USER")
            }.httpBasic {
                it.authenticationDetailsSource(userAgentWebAuthenticationDetailsSource)
            }.sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            }
        // Add Bearer token authentication filter (always enabled)
        http.addFilterBefore(bearerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        if (properties.auth.ldap.enabled) {
            val dao = DaoAuthenticationProvider()
            dao.setUserDetailsService(userDetailsService)
            dao.setPasswordEncoder(passwordEncoder)
            http.authenticationManager(ProviderManager(authenticationProvider, dao))
        }
        if (properties.auth.proxy.enabled) {
            http.addFilterBefore(authHeaderFilter, UsernamePasswordAuthenticationFilter::class.java)
        }
        if (oauth2Enabled) {
            http.oauth2Login { oauth2 ->
                oauth2.userInfoEndpoint {
                    it.userService(oauth2UserService)
                    it.oidcUserService(oidcUserService)
                }
                oauth2.authenticationDetailsSource(userAgentWebAuthenticationDetailsSource)
                oauth2
                    .loginPage("/login")
                    .defaultSuccessUrl("/?server_redirect=Y", true)
                    .failureHandler { request, response, exception ->
                        val errorMessage =
                            when (exception) {
                                is OAuth2AuthenticationException -> exception.error.errorCode
                                else -> exception.message
                            }
                        val url = "/login?server_redirect=Y&error=$errorMessage"
                        SimpleUrlAuthenticationFailureHandler(url).onAuthenticationFailure(request, response, exception)
                    }
                oauth2.redirectionEndpoint {
                }
            }
        }
        return http.build()
    }
}
