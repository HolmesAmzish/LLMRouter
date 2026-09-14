package cn.arorms.llm.router.common.responses

/**
 * Server-sent event payload for unified streaming.
 */
data class ChatStreamChunk(
    val id: String,
    val model: String,
    val index: Int,
    val delta: String,
    val finishReason: String? = null,
    val usage: Usage? = null
)
