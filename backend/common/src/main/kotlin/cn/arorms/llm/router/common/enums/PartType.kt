package cn.arorms.llm.router.common.enums

/**
 * Content part kinds supported by the canonical chat representation.
 */
enum class PartType {
    TEXT,
    IMAGE_URL,
    IMAGE_DATA,
    TOOL_RESULT
}
