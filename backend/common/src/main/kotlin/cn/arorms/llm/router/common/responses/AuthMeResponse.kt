package cn.arorms.llm.router.common.responses

/**
 * Identity information returned by the authenticated session endpoint.
 */
data class AuthMeResponse(
    val id: String,
    val username: String,
    val email: String
)
