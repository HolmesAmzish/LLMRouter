package cn.arorms.llm.router.common.responses

import cn.arorms.llm.router.common.enums.HealthStatus

/**
 * Gateway health response.
 */
data class HealthResponse(
    val status: HealthStatus,
    val version: String,
    val database: HealthStatus,
    val uptimeSeconds: Long
)
