package cn.arorms.llm.router.common.requests

/**
 * Runtime cache policy update request.
 */
data class CachePolicyRequest(
    val enabled: Boolean,
    val ttlSeconds: Long
)
