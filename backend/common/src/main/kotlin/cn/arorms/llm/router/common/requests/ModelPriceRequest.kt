package cn.arorms.llm.router.common.requests

import java.math.BigDecimal

data class ModelPriceRequest(
    val modelId: String,
    val modelName: String,
    val ownedBy: String? = null,
    val enabled: Boolean = true,
    val inputCostPerMillion: BigDecimal? = null,
    val outputCostPerMillion: BigDecimal? = null,
    val cacheReadCostPerMillion: BigDecimal? = null,
    val cacheCreationCostPerMillion: BigDecimal? = null,
    val currency: String = "USD"
)
