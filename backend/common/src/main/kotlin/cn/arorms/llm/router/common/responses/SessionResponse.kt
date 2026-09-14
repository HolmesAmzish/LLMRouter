package cn.arorms.llm.router.common.responses

import cn.arorms.llm.router.common.enums.Protocol

/**
 * Admin-facing representation of a routed conversation session.
 */
data class SessionResponse(
    val id: String,
    val title: String,
    val model: String,
    val protocol: Protocol,
    val enabled: Boolean,
    val messageCount: Int,
    val inputTokens: Long,
    val outputTokens: Long,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: String?,
    val updatedAt: String?
)
