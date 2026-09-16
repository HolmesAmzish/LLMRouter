package cn.arorms.llm.router.common.responses

import cn.arorms.llm.router.common.enums.Protocol

/**
 * A model exposed through one or more provider endpoints.
 */
data class ModelResponse(
    val id: String,
    val provider: String,
    val model: String,
    val protocol: Protocol,
    val protocols: Set<Protocol> = setOf(protocol),
    val ownedBy: String,
    val modelName: String,
    val displayName: String? = null,
    val upstreamModel: String,
    val enabled: Boolean = true
)
