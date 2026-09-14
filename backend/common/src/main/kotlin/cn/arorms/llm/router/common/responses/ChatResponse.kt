package cn.arorms.llm.router.common.responses

/**
 * Canonical chat completion response.
 */
data class ChatResponse(
    val id: String,
    val model: String,
    val choices: List<ChatChoice>,
    val usage: Usage?,
    val provider: String,
    val accountName: String?,
    val createdAt: Long,
    val cached: Boolean = false
)
