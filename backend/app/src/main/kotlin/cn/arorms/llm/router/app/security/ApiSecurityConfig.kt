package cn.arorms.llm.router.app.security

import cn.arorms.framework.security.KeycloakAuthenticationConverter
import cn.arorms.framework.security.SecurityAutoConfiguration
import cn.arorms.framework.security.UserPrincipal
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
@ConditionalOnProperty(name = ["application.security.enabled"], havingValue = "true")
@Import(SecurityAutoConfiguration::class)
class ApiSecurityConfig(
    private val authenticationConverter: KeycloakAuthenticationConverter<UserPrincipal>
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { requests ->
                requests.requestMatchers("/actuator/**").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt -> jwt.jwtAuthenticationConverter(authenticationConverter) }
            }
        return http.build()
    }
}
