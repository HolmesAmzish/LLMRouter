package cn.arorms.llm.router.app.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cors")
data class CorsProperties(
    var allowedOrigins: String = "http://localhost:5173"
) {
    fun origins(): List<String> = allowedOrigins.split(',').map(String::trim).filter(String::isNotEmpty)
}
