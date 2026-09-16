package cn.arorms.llm.router.common.responses

import cn.arorms.llm.router.common.enums.AccountStatus
import cn.arorms.llm.router.common.enums.Protocol

data class ProviderAccountResponse(
    val id: Long,
    val name: String,
    val protocolEndpoints: Map<Protocol, String>,
    val enabled: Boolean,
    val priority: Int,
    val weight: Int,
    val balanceEndpoint: String?,
    val modelMapping: Map<String, String>,
    val configuration: Map<String, String>,
    val models: List<ProviderModelResponse> = emptyList(),
    val status: AccountStatus = AccountStatus.UNKNOWN,
    val balance: Double? = null,
    val currency: String? = null,
    val hasApiKey: Boolean,
    val balanceCheckedAt: String? = null,
    val createdAt: String?,
    val updatedAt: String?
)
