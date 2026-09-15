package cn.arorms.llm.router.common.responses

import cn.arorms.llm.router.common.enums.ApiKeyType
import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * Metadata for a gateway key. The raw key is returned only when created.
 */
data class ApiKeyResponse(
    val id: Long,
    val name: String,
    val prefix: String,
    val apiKey: String? = null,
    val keyType: ApiKeyType,
    val enabled: Boolean,
    val expiresAt: OffsetDateTime?,
    val lastUsedAt: OffsetDateTime?,
    val maxBudget: BigDecimal?,
    val spend: BigDecimal,
    val rpmLimit: Int?,
    val tpmLimit: Long?,
    val models: Set<String>,
    val metadata: Map<String, String>,
    val createdAt: String?,
    val updatedAt: String?
)
