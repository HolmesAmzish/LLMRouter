package cn.arorms.llm.router.common.responses

/**
 * Generic response used by paginated admin endpoints.
 */
data class PageResponse<T>(
    val content: List<T>,
    val total: Long,
    val page: Int,
    val size: Int,
    val totalPages: Int,
    val isLast: Boolean
) {
    companion object {
        fun <T, R> of(content: List<R>, total: Long, request: cn.arorms.llm.router.common.requests.PageRequest): PageResponse<R> {
            val size = request.size.coerceAtLeast(1)
            val totalPages = if (total == 0L) 0 else ((total + size - 1) / size).toInt()
            return PageResponse(
                content = content,
                total = total,
                page = request.page,
                size = size,
                totalPages = totalPages,
                isLast = request.page >= totalPages - 1
            )
        }
    }
}
