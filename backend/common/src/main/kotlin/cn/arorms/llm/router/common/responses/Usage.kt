package cn.arorms.llm.router.common.responses

import cn.arorms.llm.router.common.enums.TokenInputSemantics

/**
 * Detailed token usage for an inference response.
 */
data class Usage(
    val inputTokens: Long,
    val outputTokens: Long,
    val totalTokens: Long = inputTokens + outputTokens,
    val cacheReadTokens: Long = 0,
    val cacheCreationTokens: Long = 0,
    val reasoningTokens: Long? = null,
    val inputTokenSemantics: TokenInputSemantics = TokenInputSemantics.UNKNOWN,
    val tokenDetails: Map<String, Long> = emptyMap(),
    val costCents: Long? = null
) {
    /**
     * Cache-normalized token total used for usage analysis. Unlike provider
     * total_tokens, this does not double-count cache hits under inputTokens.
     */
    val realTotalTokens: Long
        get() = when (inputTokenSemantics) {
            TokenInputSemantics.TOTAL -> (inputTokens - cacheReadTokens).coerceAtLeast(0)
            else -> inputTokens
        } + outputTokens + cacheReadTokens + cacheCreationTokens
}
