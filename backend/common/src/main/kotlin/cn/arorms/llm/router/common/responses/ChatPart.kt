package cn.arorms.llm.router.common.responses

import cn.arorms.llm.router.common.enums.PartType

/**
 * Canonical multimodal or tool-result content part.
 */
data class ChatPart(
    val type: PartType,
    val text: String? = null,
    val imageUrl: String? = null,
    val mimeType: String? = null,
    val data: String? = null,
    val toolCallId: String? = null,
    val name: String? = null
)
