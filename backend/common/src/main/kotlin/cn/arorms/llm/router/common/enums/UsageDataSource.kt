package cn.arorms.llm.router.common.enums

/**
 * Where a usage record's response was produced.
 */
enum class UsageDataSource {
    UPSTREAM,
    ROUTER_CACHE
}
