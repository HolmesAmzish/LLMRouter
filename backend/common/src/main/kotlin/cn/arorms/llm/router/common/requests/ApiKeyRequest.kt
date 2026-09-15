package cn.arorms.llm.router.common.requests

import java.time.OffsetDateTime

/**
 * Request to issue a gateway API key for external clients.
 */
data class ApiKeyRequest(
    val name: String,
    val expiresAt: OffsetDateTime? = null
)
