package cn.arorms.llm.router.common.responses

import cn.arorms.llm.router.common.enums.AccountStatus

/**
 * Balance query response for one provider account.
 */
data class AccountBalanceResponse(
    val accountId: Long,
    val balance: Double?,
    val currency: String?,
    val status: AccountStatus,
    val checkedAt: String
)
