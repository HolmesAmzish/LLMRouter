package cn.arorms.llm.router.common.responses

/**
 * One model exposed by the router or returned by an upstream provider.
 */
data class ModelResponse(
    val id: String,
    val ownedBy: String,
    val displayName: String? = null,
    val enabled: Boolean = true,
    val supportedFeatures: Set<String> = emptySet()
)
