package cn.arorms.llm.router.common.enums

/**
 * Virtual key scope. Management keys are reserved for future admin API keys.
 */
enum class ApiKeyType {
    GATEWAY,
    READ_ONLY,
    MANAGEMENT
}
