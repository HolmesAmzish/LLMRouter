package cn.arorms.llm.router.common.requests

/**
 * Filter and paging options for usage queries.
 */
data class UsageFilter(
    val from: String? = null,
    val to: String? = null,
    val provider: String? = null,
    val accountName: String? = null,
    val model: String? = null,
    val sessionId: String? = null,
    val page: Int = 0,
    val size: Int = 20
)
