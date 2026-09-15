package cn.arorms.llm.router.app.mappers

import cn.arorms.llm.router.app.entities.ProviderAccount
import cn.arorms.llm.router.common.enums.AccountStatus
import cn.arorms.llm.router.common.enums.Protocol
import cn.arorms.llm.router.common.responses.ProviderAccountResponse

object ProviderAccountMapper {
    fun toResponse(account: ProviderAccount): ProviderAccountResponse = ProviderAccountResponse(
        id = account.id ?: 0L,
        name = account.name,
        protocolEndpoints = account.protocolEndpoints.mapNotNull { (name, url) ->
            runCatching { Protocol.valueOf(name) }.getOrNull()?.let { it to url }
        }.toMap().ifEmpty { mapOf(account.defaultProtocol to account.baseUrl) },
        enabled = account.enabled,
        priority = account.priority,
        weight = account.weight,
        balanceEndpoint = account.balanceEndpoint,
        modelMapping = account.modelMapping,
        configuration = account.configuration,
        status = account.status.uppercase().let {
            runCatching { AccountStatus.valueOf(it) }.getOrDefault(AccountStatus.UNKNOWN)
        },
        balance = account.balance,
        currency = account.currency,
        hasApiKey = account.apiKey.isNotBlank(),
        balanceCheckedAt = account.balanceCheckedAt?.toString(),
        createdAt = account.createdAt?.toString(),
        updatedAt = account.updatedAt?.toString()
    )
}
