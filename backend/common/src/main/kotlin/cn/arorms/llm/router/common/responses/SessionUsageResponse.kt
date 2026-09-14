package cn.arorms.llm.router.common.responses

/**
 * Aggregated token usage for one session.
 */
data class SessionUsageResponse(
    val sessionId: String,
    val inputTokens: Long,
    val outputTokens: Long,
    val totalTokens: Long,
    val messageCount: Int
)
