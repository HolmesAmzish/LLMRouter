package cn.arorms.llm.router.common.responses

/**
 * One model exposed by the router. `id` is the qualified public model name in
 * `<provider>/<model>` format.
 */
data class ModelResponse(
    val id: String,
    val provider: String,
    val model: String,
    val ownedBy: String,
    val displayName: String? = null,
    val enabled: Boolean = true,
    val supportedFeatures: Set<String> = emptySet()
)
