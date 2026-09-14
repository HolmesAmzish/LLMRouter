package cn.arorms.llm.router.app.service

import cn.arorms.llm.router.app.dto.Mappers
import cn.arorms.llm.router.app.entity.ProviderAccountEntity
import cn.arorms.llm.router.app.repository.ProviderAccountRepository
import cn.arorms.llm.router.common.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class AccountService(
    private val repository: ProviderAccountRepository,
    private val httpClient: GatewayHttpClient,
    private val mapper: ObjectMapper
) {
    @Transactional
    fun create(request: ProviderAccountRequest): ProviderAccountResponse = repository.save(
        ProviderAccountEntity(
            name = request.name,
            protocol = request.protocol,
            baseUrl = request.baseUrl.trimEnd('/'),
            apiKey = request.apiKey,
            enabled = request.enabled,
            priority = request.priority,
            weight = request.weight,
            balanceEndpoint = request.balanceEndpoint?.trim(),
            modelMapping = request.modelMapping,
            configuration = request.configuration
        )
    ).let { Mappers.toResponse(it) }

    @Transactional
    fun update(id: Long, patch: ProviderAccountPatch): ProviderAccountResponse {
        val account = repository.findById(id).orElseThrow()
        patch.name?.let { account.name = it }
        patch.baseUrl?.let { account.baseUrl = it.trimEnd('/') }
        patch.apiKey?.takeIf { it.isNotBlank() }?.let { account.apiKey = it }
        patch.enabled?.let { account.enabled = it }
        patch.priority?.let { account.priority = it }
        patch.weight?.let { account.weight = it }
        patch.balanceEndpoint?.let { account.balanceEndpoint = it }
        patch.modelMapping?.let { account.modelMapping = it }
        patch.configuration?.let { account.configuration = it }
        return Mappers.toResponse(account)
    }

    @Transactional
    fun delete(id: Long) = repository.deleteById(id)

    @Transactional
    fun get(id: Long): ProviderAccountResponse = repository.findById(id).orElseThrow().let(Mappers::toResponse)

    @Transactional
    fun list(provider: String?, enabled: Boolean? = null): List<ProviderAccountResponse> {
        val all = repository.findAll().sortedBy { it.priority }
        return all.asSequence()
            .filter { provider == null || it.protocol.name.equals(provider, true) }
            .filter { enabled == null || it.enabled == enabled }
            .map(Mappers::toResponse)
            .toList()
    }

    @Transactional
    fun selectedAccounts(protocol: Protocol): List<ProviderAccountEntity> =
        repository.findByEnabledTrueOrderByPriorityAscIdAsc().filter { it.protocol == protocol }

    suspend fun refreshBalance(id: Long): AccountBalanceResponse = withContext(Dispatchers.IO) {
        val account = repository.findById(id).orElseThrow()
        val endpoint = account.balanceEndpoint ?: defaultBalanceEndpoint(account.baseUrl)
            ?: return@withContext AccountBalanceResponse(id, account.balance, account.currency, "unsupported", OffsetDateTime.now().toString())
        val json = httpClient.getJson(endpoint, mapOf("Authorization" to "Bearer ${account.apiKey}"))
        val parsed = parseBalance(mapper.readTree(json))
        account.balance = parsed.first
        account.currency = parsed.second
        account.status = if (parsed.first != null) "active" else "unknown"
        account.balanceCheckedAt = OffsetDateTime.now()
        AccountBalanceResponse(id, account.balance, account.currency, account.status, account.balanceCheckedAt.toString())
    }

    private fun defaultBalanceEndpoint(baseUrl: String): String? = when {
        baseUrl.contains("openrouter.ai") -> "$baseUrl/api/v1/key"
        baseUrl.contains("api.deepseek.com") -> "$baseUrl/user/balance"
        else -> null
    }

    private fun parseBalance(body: JsonNode): Pair<Double?, String?> {
        val candidates = listOf(
            body.path("data").path("limits").path("remaining"),
            body.path("data").path("balance"),
            body.path("info").path("balance"),
            body.path("balance_infos").get(0).path("total_balance"),
            body.path("balance")
        )
        val amount = candidates.firstOrNull { it.isNumber || it.isTextual }?.asDouble()
        val currency = body.path("data").path("usage").path("currency").asText(
            body.path("balance_infos").get(0).path("currency").asText(
                body.path("currency").asText("USD")
            )
        )
        return amount to currency
    }
}
