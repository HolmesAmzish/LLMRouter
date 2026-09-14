package cn.arorms.llm.router.common.responses

import cn.arorms.llm.router.common.enums.Protocol

/**
 * Result of synchronizing one account's model catalog.
 */
data class ModelSyncResponse(
    val accountId: Long,
    val protocol: Protocol,
    val syncedCount: Int,
    val models: List<ModelResponse>,
    val syncedAt: String
)
