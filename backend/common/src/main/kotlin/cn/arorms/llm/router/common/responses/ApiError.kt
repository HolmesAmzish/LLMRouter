package cn.arorms.llm.router.common.responses

/**
 * Stable error contract exposed by the gateway and admin APIs.
 */
data class ApiError(
    val code: String,
    val message: String,
    val details: List<String> = emptyList(),
    val timestamp: String
)
