package cn.arorms.llm.router.common.responses

/**
 * Simple acknowledgement response.
 */
data class SimpleResponse(
    val ok: Boolean,
    val message: String? = null
)
