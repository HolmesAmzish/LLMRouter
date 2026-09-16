package cn.arorms.llm.router.common.enums

/**
 * How the upstream input token counter treats prompt-cache tokens.
 *
 * TOTAL means inputTokens already contains cacheReadTokens.
 * FRESH means inputTokens excludes cacheReadTokens and cacheCreationTokens.
 */
enum class TokenInputSemantics {
    UNKNOWN,
    TOTAL,
    FRESH
}
