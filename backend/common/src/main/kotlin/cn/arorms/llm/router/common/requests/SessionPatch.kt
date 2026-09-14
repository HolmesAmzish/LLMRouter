package cn.arorms.llm.router.common.requests

/**
 * Partial-update payload for a routed conversation session.
 */
data class SessionPatch(
    val title: String? = null,
    val model: String? = null,
    val enabled: Boolean? = null,
    val metadata: Map<String, String>? = null
)
