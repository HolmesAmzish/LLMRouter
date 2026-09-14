package cn.arorms.llm.router.common.responses

import cn.arorms.llm.router.common.enums.AccountStatus
import cn.arorms.llm.router.common.enums.Protocol

/**
 * Result of testing connectivity and credentials for an account.
 */
data class AccountTestResponse(
    val accountId: Long,
    val protocol: Protocol,
    val ok: Boolean,
    val status: AccountStatus,
    val message: String? = null,
    val latencyMs: Long? = null
)
