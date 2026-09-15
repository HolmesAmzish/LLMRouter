package cn.arorms.llm.router.app.security

import cn.arorms.llm.router.app.services.ApiKeyService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class ApiKeyAuthenticationFilter(private val apiKeyService: ApiKeyService) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (!request.requestURI.startsWith("/v1/")) {
            filterChain.doFilter(request, response)
            return
        }

        val rawKey = extractApiKey(request)
        val apiKey = rawKey?.let(apiKeyService::authenticate)
        if (apiKey == null) {
            response.status = HttpStatus.UNAUTHORIZED.value()
            response.setHeader("WWW-Authenticate", "ApiKey")
            response.contentType = "application/json"
            response.writer.write("""{"error":{"code":"invalid_api_key","message":"A valid gateway API key is required"}}""")
            return
        }

        request.setAttribute("gateway.apiKey", apiKey)
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
            apiKey.name,
            rawKey,
            listOf(SimpleGrantedAuthority("ROLE_GATEWAY"))
        )
        filterChain.doFilter(request, response)
    }

    private fun extractApiKey(request: HttpServletRequest): String? {
        request.getHeader("X-Api-Key")?.takeIf { it.isNotBlank() }?.let { return it }
        val authorization = request.getHeader("Authorization") ?: return null
        val token = authorization.takeIf { it.startsWith("Bearer ", true) }?.substring(7)?.trim()
        return token?.takeIf { it.startsWith("sk-router-") }
    }
}
