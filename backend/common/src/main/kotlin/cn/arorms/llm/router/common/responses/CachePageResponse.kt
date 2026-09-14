package cn.arorms.llm.router.common.responses

/**
 * Paged cache entries.
 */
data class CachePageResponse(
    val content: List<CacheEntryResponse>,
    val total: Long,
    val page: Int,
    val size: Int
)
