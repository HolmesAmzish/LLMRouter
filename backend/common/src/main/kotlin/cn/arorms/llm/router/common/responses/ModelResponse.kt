package cn.arorms.llm.router.common.responses

import cn.arorms.llm.router.common.enums.Protocol

/**
 * One model deployment exposed by the router.
 */
data class ModelResponse(
    val id: String,
    val provider: String,
    val model: String,
    val protocol: Protocol,
    val protocols: Set<Protocol> = setOf(protocol),
    val ownedBy: String,
    val displayName: String? = null,
    val upstreamModel: String,
    val enabled: Boolean = true,
    val maxContextTokens: Int? = null,
    val maxOutputTokens: Int? = null,
    val supportedFeatures: Set<String> = emptySet()
)
