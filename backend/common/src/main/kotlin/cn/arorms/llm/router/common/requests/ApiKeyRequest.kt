package cn.arorms.llm.router.common.requests

import java.time.OffsetDateTime

/**
 * Request to issue a virtual gateway key.
 */
data class ApiKeyRequest(
    val name: String,
    val expiresAt: OffsetDateTime? = null,
    val maxBudget: Double? = null,
    val rpmLimit: Int? = null,
    val tpmLimit: Long? = null,
    val models: Set<String> = emptySet(),
    val metadata: Map<String, String> = emptyMap()
)
