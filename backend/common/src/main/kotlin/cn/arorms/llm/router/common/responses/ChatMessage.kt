package cn.arorms.llm.router.common.responses

/**
 * Canonical chat message shared by requests and responses.
 */
data class ChatMessage(
    val role: String,
    val parts: List<ChatPart> = emptyList(),
    val toolCalls: List<ToolCall> = emptyList()
)
