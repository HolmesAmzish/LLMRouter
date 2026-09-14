package cn.arorms.llm.router.common

data class SessionRequest(
    val title: String,
    val model: String,
    val protocol: Protocol,
    val enabled: Boolean = true
)

data class SessionResponse(
    val id: String,
    val title: String,
    val model: String,
    val protocol: Protocol,
    val enabled: Boolean,
    val messageCount: Int,
    val inputTokens: Long,
    val outputTokens: Long,
    val createdAt: String?,
    val updatedAt: String?
)

data class UsageFilter(
    val from: String? = null,
    val to: String? = null,
    val provider: String? = null,
    val model: String? = null,
    val sessionId: String? = null,
    val page: Int = 0,
    val size: Int = 20
)

data class UsageResponse(
    val id: Long,
    val sessionId: String?,
    val provider: String,
    val accountName: String?,
    val model: String,
    val protocol: Protocol,
    val inputTokens: Long,
    val outputTokens: Long,
    val totalTokens: Long,
    val costCents: Long?,
    val latencyMs: Long,
    val status: String,
    val createdAt: String?
)

data class UsagePageResponse(
    val content: List<UsageResponse>,
    val total: Long,
    val page: Int,
    val size: Int,
    val totalTokens: Long
)
