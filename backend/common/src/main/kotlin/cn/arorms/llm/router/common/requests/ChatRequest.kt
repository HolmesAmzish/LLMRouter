package cn.arorms.llm.router.common.requests

import cn.arorms.llm.router.common.enums.CachePolicy
import cn.arorms.llm.router.common.enums.ThinkingEffort
import cn.arorms.llm.router.common.responses.ChatMessage
import cn.arorms.llm.router.common.responses.ToolDefinition

/**
 * Provider-neutral chat request accepted by the gateway.
 */
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val maxTokens: Int? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val tools: List<ToolDefinition> = emptyList(),
    val thinkingEffort: ThinkingEffort = ThinkingEffort.NONE,
    val sessionId: String? = null,
    val cachePolicy: CachePolicy = CachePolicy.DEFAULT,
    val stream: Boolean = false,
    val extra: Map<String, Any?> = emptyMap()
)
