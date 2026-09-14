package cn.arorms.llm.router.common.responses

/**
 * OpenAI-compatible model list response.
 */
data class ModelListResponse(
    val provider: String,
    val objectValue: String = "list",
    val data: List<ModelResponse>
)
