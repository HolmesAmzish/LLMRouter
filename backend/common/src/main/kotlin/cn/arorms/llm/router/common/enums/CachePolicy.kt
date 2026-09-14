package cn.arorms.llm.router.common.enums

/**
 * Desired cache behavior for an inference request.
 */
enum class CachePolicy {
    DEFAULT,
    ENABLED,
    DISABLED,
    BYPASS
}
