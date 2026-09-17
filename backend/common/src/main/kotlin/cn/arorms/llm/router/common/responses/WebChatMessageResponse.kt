package cn.arorms.llm.router.common.responses

import cn.arorms.llm.router.common.enums.ChatMessageStatus
import cn.arorms.llm.router.common.enums.Protocol

data class WebChatMessageResponse(
    val id: Long,
    val role: String,
    val content: String,
    val model: String?,
    val protocol: Protocol?,
    val status: ChatMessageStatus,
    val createdAt: String?
)
