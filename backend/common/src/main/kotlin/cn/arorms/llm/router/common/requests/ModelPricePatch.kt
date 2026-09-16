package cn.arorms.llm.router.common.requests

import java.math.BigDecimal

data class ModelPricePatch(
    val modelName: String? = null,
    val ownedBy: String? = null,
    val enabled: Boolean? = null,
    val inputCostPerMillion: BigDecimal? = null,
    val outputCostPerMillion: BigDecimal? = null,
    val cacheReadCostPerMillion: BigDecimal? = null,
    val cacheCreationCostPerMillion: BigDecimal? = null,
    val currency: String? = null
)
