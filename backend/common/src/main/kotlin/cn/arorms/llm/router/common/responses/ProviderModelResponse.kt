package cn.arorms.llm.router.common.responses

data class ProviderModelResponse(
    val id: Long,
    val providerId: Long,
    val providerName: String,
    val modelId: String,
    val modelName: String,
    val modelPriceId: Long?,
    val priced: Boolean,
    val enabled: Boolean
)
