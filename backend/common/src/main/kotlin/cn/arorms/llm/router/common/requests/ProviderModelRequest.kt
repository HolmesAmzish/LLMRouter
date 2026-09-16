package cn.arorms.llm.router.common.requests

data class ProviderModelRequest(
    val modelId: String,
    val modelName: String? = null,
    val modelPriceId: Long? = null,
    val enabled: Boolean = true
)
