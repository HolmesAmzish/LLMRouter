package cn.arorms.llm.router.common.requests

/**
 * Canonical pagination request.
 */
data class PageRequest(
    val page: Int = 0,
    val size: Int = 20
) {
    init {
        require(page >= 0) { "page must not be negative" }
        require(size in 1..200) { "size must be between 1 and 200" }
    }
}
