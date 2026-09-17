package cn.arorms.llm.router.app.security

import cn.arorms.framework.security.KeycloakAuthenticationConverter
import cn.arorms.framework.security.SecurityAutoConfiguration
import cn.arorms.framework.security.UserPrincipal
import cn.arorms.llm.router.app.services.ApiKeyService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter


@Configuration
@ConditionalOnProperty(name = ["application.security.enabled"], havingValue = "true")
@Import(SecurityAutoConfiguration::class)
class ApiSecurityConfig(
    private val authenticationConverter: KeycloakAuthenticationConverter<UserPrincipal>,
    private val apiKeyService: ApiKeyService
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { requests ->
                requests
                    .requestMatchers("/actuator/**", "/error").permitAll()
                    .requestMatchers("/v1/**").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.bearerTokenResolver { request ->
                    request.getHeader("Authorization")
                        ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
                        ?.substring(7)
                        ?.trim()
                        ?.takeUnless { it.startsWith("sk-router-") }
                }
                oauth2.jwt { jwt -> jwt.jwtAuthenticationConverter(authenticationConverter) }
            }
            .addFilterBefore(
                ApiKeyAuthenticationFilter(apiKeyService),
                BearerTokenAuthenticationFilter::class.java
            )
        return http.build()
    }
}
