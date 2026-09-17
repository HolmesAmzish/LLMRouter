package cn.arorms.llm.router.common.responses

import cn.arorms.llm.router.common.enums.Currency
import java.math.BigDecimal

data class ModelPriceResponse(
    val id: Long,
    val modelId: String,
    val modelName: String,
    val ownedBy: String?,
    val enabled: Boolean,
    val inputCostPerMillion: BigDecimal?,
    val outputCostPerMillion: BigDecimal?,
    val cacheReadCostPerMillion: BigDecimal?,
    val cacheCreationCostPerMillion: BigDecimal?,
    val currency: Currency,
    val createdAt: String?,
    val updatedAt: String?
)
