package cn.arorms.llm.router.common.responses

/**
 * One canonical chat completion choice.
 */
data class ChatChoice(
    val index: Int,
    val message: ChatMessage,
    val finishReason: String? = null
)
