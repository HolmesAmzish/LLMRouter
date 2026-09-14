package cn.arorms.llm.router.common.responses

/**
 * Collection response for sessions.
 */
data class SessionListResponse(
    val data: List<SessionResponse>,
    val total: Long
)
