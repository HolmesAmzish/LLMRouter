package cn.arorms.llm.router.common.responses

import cn.arorms.llm.router.common.enums.Protocol

/**
 * One cached inference response.
 */
data class CacheEntryResponse(
    val fingerprint: String,
    val sessionId: String?,
    val protocol: Protocol,
    val model: String,
    val hitCount: Long,
    val expiresAt: String,
    val createdAt: String?,
    val updatedAt: String?
)
