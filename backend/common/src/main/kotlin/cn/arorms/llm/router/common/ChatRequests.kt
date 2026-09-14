package cn.arorms.llm.router.common

data class ChatMessage(
    val role: String,
    val parts: List<ChatPart> = emptyList()
)

data class ChatPart(
    val type: PartType,
    val text: String? = null,
    val imageUrl: String? = null,
    val mimeType: String? = null,
    val data: String? = null,
    val toolCallId: String? = null,
    val name: String? = null
)

enum class PartType { TEXT, IMAGE_URL, IMAGE_DATA, TOOL_RESULT }

data class ToolDefinition(
    val name: String,
    val description: String? = null,
    val schema: Map<String, Any?>? = null
)

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val maxTokens: Int? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val tools: List<ToolDefinition> = emptyList(),
    val thinkingEffort: ThinkingEffort = ThinkingEffort.NONE,
    val sessionId: String? = null,
    val stream: Boolean = false,
    val extra: Map<String, Any?> = emptyMap()
)

data class ToolCall(
    val id: String? = null,
    val name: String,
    val arguments: String
)

data class ChatChoice(
    val index: Int,
    val message: ChatMessage,
    val finishReason: String? = null
)

data class Usage(
    val inputTokens: Long,
    val outputTokens: Long,
    val totalTokens: Long = inputTokens + outputTokens,
    val costCents: Long? = null
)

data class ChatResponse(
    val id: String,
    val model: String,
    val choices: List<ChatChoice>,
    val usage: Usage?,
    val provider: String,
    val accountName: String?,
    val createdAt: Long
)
