package cn.arorms.llm.router.common.responses

import cn.arorms.llm.router.common.enums.Protocol

/**
 * One recorded inference usage event.
 */
data class UsageResponse(
    val id: Long,
    val requestId: String,
    val sessionId: String?,
    val apiKeyId: Long?,
    val apiKeyName: String?,
    val provider: String,
    val accountName: String?,
    val model: String,
    val publicModel: String,
    val upstreamModel: String,
    val protocol: Protocol,
    val inputTokens: Long,
    val outputTokens: Long,
    val totalTokens: Long,
    val costCents: Long?,
    val latencyMs: Long,
    val firstTokenMs: Long? = null,
    val apiBase: String? = null,
    val cacheHit: Boolean = false,
    val status: String,
    val statusCode: Int? = null,
    val createdAt: String?
)
