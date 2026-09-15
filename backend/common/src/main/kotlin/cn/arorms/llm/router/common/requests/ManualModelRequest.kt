package cn.arorms.llm.router.common.requests

import cn.arorms.llm.router.common.enums.Protocol

/**
 * Request to register one explicit model deployment.
 */
data class ManualModelRequest(
    val accountId: Long,
    val model: String,
    val protocol: Protocol = Protocol.OPENAI,
    val upstreamModel: String? = null,
    val displayName: String? = null,
    val ownedBy: String? = null,
    val enabled: Boolean = true,
    val maxContextTokens: Int? = null,
    val maxOutputTokens: Int? = null
)
