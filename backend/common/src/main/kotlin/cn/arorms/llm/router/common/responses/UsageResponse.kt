package cn.arorms.llm.router.common.responses

import cn.arorms.llm.router.common.enums.Protocol

/**
 * One recorded inference usage event.
 */
data class UsageResponse(
    val id: Long,
    val sessionId: String?,
    val provider: String,
    val accountName: String?,
    val model: String,
    val protocol: Protocol,
    val inputTokens: Long,
    val outputTokens: Long,
    val totalTokens: Long,
    val costCents: Long?,
    val latencyMs: Long,
    val status: String,
    val createdAt: String?
)
