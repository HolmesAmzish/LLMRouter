package cn.arorms.llm.router.common.responses

import java.time.OffsetDateTime

/**
 * Metadata for a gateway API key. `apiKey` is only returned at creation time.
 */
data class ApiKeyResponse(
    val id: Long,
    val name: String,
    val prefix: String,
    val apiKey: String? = null,
    val enabled: Boolean,
    val expiresAt: OffsetDateTime?,
    val lastUsedAt: OffsetDateTime?,
    val createdAt: String?,
    val updatedAt: String?
)
