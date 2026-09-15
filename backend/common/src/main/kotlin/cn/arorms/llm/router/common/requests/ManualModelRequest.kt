package cn.arorms.llm.router.common.requests

/**
 * Request to manually register a model provided by an upstream account.
 */
data class ManualModelRequest(
    val accountId: Long,
    val model: String,
    val displayName: String? = null,
    val ownedBy: String? = null,
    val enabled: Boolean = true
)
