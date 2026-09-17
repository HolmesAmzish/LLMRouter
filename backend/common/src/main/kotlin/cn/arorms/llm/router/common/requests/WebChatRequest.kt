package cn.arorms.llm.router.common.requests

import cn.arorms.llm.router.common.enums.Protocol
import cn.arorms.llm.router.common.enums.ThinkingEffort

/**
 * One message submitted by a web chat UI. Conversation history is loaded by
 * the server from chat_messages and is not trusted from the browser.
 */
data class WebChatRequest(
    val content: String,
    val model: String,
    val protocol: Protocol,
    val temperature: Double? = null,
    val thinkingEffort: ThinkingEffort = ThinkingEffort.NONE,
    val imageDataUrl: String? = null,
    val tools: List<Map<String, Any?>> = emptyList()
)
