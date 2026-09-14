package cn.arorms.llm.router.app.adapters

/**
 * A protocol-agnostic Server-Sent Events frame before it is handed to Reactor.
 */
data class SseFrame(
    val event: String? = null,
    val data: String
)
