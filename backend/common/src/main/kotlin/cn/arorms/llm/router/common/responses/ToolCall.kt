package cn.arorms.llm.router.common.responses

/**
 * Tool invocation requested by a model.
 */
data class ToolCall(
    val id: String? = null,
    val name: String,
    val arguments: String
)
