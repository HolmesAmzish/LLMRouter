package cn.arorms.llm.router.common.responses

/**
 * Paged usage records with the aggregate token total for the filter.
 */
data class UsagePageResponse(
    val content: List<UsageResponse>,
    val total: Long,
    val page: Int,
    val size: Int,
    val totalTokens: Long
)
