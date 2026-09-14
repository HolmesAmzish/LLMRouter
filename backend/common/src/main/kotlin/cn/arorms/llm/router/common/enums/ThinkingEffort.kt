package cn.arorms.llm.router.common.enums

/**
 * Normalized reasoning effort. Each adapter maps this to provider-native settings.
 */
enum class ThinkingEffort {
    NONE,
    MINIMAL,
    LOW,
    MEDIUM,
    HIGH
}
