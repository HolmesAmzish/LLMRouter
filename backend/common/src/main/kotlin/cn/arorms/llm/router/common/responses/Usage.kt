package cn.arorms.llm.router.common.responses

/**
 * Token usage for an inference response.
 */
data class Usage(
    val inputTokens: Long,
    val outputTokens: Long,
    val totalTokens: Long = inputTokens + outputTokens,
    val costCents: Long? = null
)
