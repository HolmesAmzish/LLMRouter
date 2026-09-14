package cn.arorms.llm.router.common.enums

/**
 * Well-known account states returned to admin clients.
 */
enum class AccountStatus {
    UNKNOWN,
    ACTIVE,
    INVALID_KEY,
    RATE_LIMITED,
    DISABLED,
    UNSUPPORTED
}
