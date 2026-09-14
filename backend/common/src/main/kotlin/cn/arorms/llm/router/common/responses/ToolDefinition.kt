package cn.arorms.llm.router.common.responses

/**
 * Provider-neutral AI tool definition.
 */
data class ToolDefinition(
    val name: String,
    val description: String? = null,
    val schema: Map<String, Any?>? = null
)
