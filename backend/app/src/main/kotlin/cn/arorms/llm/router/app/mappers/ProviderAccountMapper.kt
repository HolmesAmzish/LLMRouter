package cn.arorms.llm.router.app.mappers

import cn.arorms.llm.router.app.entities.ProviderAccount
import cn.arorms.llm.router.app.entities.ProviderModel
import cn.arorms.llm.router.common.enums.AccountStatus
import cn.arorms.llm.router.common.enums.Protocol
import cn.arorms.llm.router.common.responses.ProviderAccountResponse
import cn.arorms.llm.router.common.responses.ProviderModelResponse

object ProviderAccountMapper {
    fun toResponse(
        account: ProviderAccount,
        models: List<ProviderModel> = emptyList()
    ): ProviderAccountResponse = ProviderAccountResponse(
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
        models = models.map { it.toResponse() },
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

    fun ProviderModel.toResponse() = ProviderModelResponse(
        id = id ?: 0L,
        providerId = providerId,
        providerName = providerName,
        modelId = modelId,
        modelName = modelName,
        modelPriceId = modelDefinitionId,
        priced = modelDefinitionId != null,
        enabled = enabled
    )
}
