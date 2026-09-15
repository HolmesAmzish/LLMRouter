package cn.arorms.llm.router.common.enums

/**
 * API protocol accepted by the router or spoken by an upstream endpoint.
 *
 * OPENAI means the Chat Completions API. OPENAI_RESPONSES means the Responses API.
 */
enum class Protocol {
    OPENAI,
    OPENAI_RESPONSES,
    ANTHROPIC

    // Other protocols are intentionally disabled for now.
    // GEMINI
}
