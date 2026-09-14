package cn.arorms.llm.router.common.requests

import cn.arorms.llm.router.common.enums.Protocol

/**
 * Request payload for creating a routed conversation session.
 */
data class SessionRequest(
    val title: String,
    val model: String,
    val protocol: Protocol,
    val enabled: Boolean = true,
    val metadata: Map<String, String> = emptyMap()
)
