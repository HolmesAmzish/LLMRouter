package cn.arorms.llm.router.common.responses

/**
 * Cache hit and size statistics.
 */
data class CacheStatsResponse(
    val enabled: Boolean,
    val entryCount: Long,
    val hitCount: Long,
    val missCount: Long,
    val hitRate: Double? = null,
    val oldestExpiresAt: String? = null
)
