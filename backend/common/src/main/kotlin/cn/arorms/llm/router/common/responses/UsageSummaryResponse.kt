package cn.arorms.llm.router.common.responses

/**
 * Aggregated usage summary.
 */
data class UsageSummaryResponse(
    val totalRequests: Long,
    val totalInputTokens: Long,
    val totalOutputTokens: Long,
    val totalTokens: Long,
    val totalCostCents: Long? = null
)
