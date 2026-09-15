package cn.arorms.llm.router.app.services

import cn.arorms.llm.router.app.entities.ProviderAccount
import cn.arorms.llm.router.app.mappers.ProviderAccountMapper
import cn.arorms.llm.router.app.repositories.ProviderAccountRepository
import cn.arorms.llm.router.common.enums.AccountStatus
import cn.arorms.llm.router.common.enums.Protocol
import cn.arorms.llm.router.common.requests.ProviderAccountPatch
import cn.arorms.llm.router.common.requests.ProviderAccountRequest
import cn.arorms.llm.router.common.responses.AccountBalanceResponse
import cn.arorms.llm.router.common.responses.ProviderAccountResponse
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URI
import java.time.OffsetDateTime

@Service
class AccountService(
    private val repository: ProviderAccountRepository,
    private val httpClient: GatewayHttpClient,
    private val mapper: ObjectMapper
) {
    @Transactional
    fun create(request: ProviderAccountRequest): ProviderAccountResponse {
        require(request.protocolEndpoints.isNotEmpty()) {
            "At least one protocol endpoint is required"
        }

        val endpoints = request.protocolEndpoints
            .mapValues { (_, url) -> url.trim() }
        endpoints.values.forEach(::validateEndpoint)

        val account = repository.save(
            ProviderAccount(
                name = request.name.trim(),
                defaultProtocol = endpoints.keys.first(),
                baseUrl = endpoints.values.first(),
                protocolEndpoints = endpoints.mapKeys { (protocol, _) -> protocol.name },
                apiKey = request.apiKey,
                enabled = request.enabled,
                priority = request.priority,
                weight = request.weight,
                balanceEndpoint = request.balanceEndpoint?.trim()?.takeIf { it.isNotEmpty() },
                modelMapping = request.modelMapping,
                configuration = request.configuration
            )
        )
        return ProviderAccountMapper.toResponse(account)
    }

    @Transactional
    fun update(id: Long, patch: ProviderAccountPatch): ProviderAccountResponse {
        val account = repository.findById(id).orElseThrow()
        patch.name?.takeIf { it.isNotBlank() }?.let { account.name = it.trim() }
        patch.protocolEndpoints?.takeIf { it.isNotEmpty() }?.let { requested ->
            val endpoints = requested.mapValues { (_, url) -> url.trim() }
            endpoints.values.forEach(::validateEndpoint)
            account.protocolEndpoints = endpoints.mapKeys { (protocol, _) -> protocol.name }
            account.defaultProtocol = endpoints.keys.first()
            account.baseUrl = endpoints.values.first()
        }
        patch.apiKey?.takeIf { it.isNotBlank() }?.let { account.apiKey = it }
        patch.enabled?.let { account.enabled = it }
        patch.priority?.let { account.priority = it }
        patch.weight?.let { account.weight = it }
        patch.balanceEndpoint?.let { account.balanceEndpoint = it.trim().takeIf { value -> value.isNotEmpty() } }
        patch.modelMapping?.let { account.modelMapping = it }
        patch.configuration?.let { account.configuration = it }
        return ProviderAccountMapper.toResponse(account)
    }

    @Transactional
    fun delete(id: Long) = repository.deleteById(id)

    @Transactional
    fun get(id: Long): ProviderAccountResponse = repository.findById(id).orElseThrow().let(ProviderAccountMapper::toResponse)

    @Transactional
    fun list(provider: String?, enabled: Boolean? = null): List<ProviderAccountResponse> {
        val all = repository.findAll().sortedBy { it.priority }
        return all.asSequence()
            .filter { provider == null || Protocol.entries.any { protocol -> protocol.name.equals(provider, true) && it.supports(protocol) } }
            .filter { enabled == null || it.enabled == enabled }
            .map(ProviderAccountMapper::toResponse)
            .toList()
    }

    @Transactional
    fun selectedAccounts(protocol: Protocol): List<ProviderAccount> =
        repository.findByEnabledTrueOrderByPriorityAscIdAsc().filter { it.supports(protocol) }

    suspend fun refreshBalance(id: Long): AccountBalanceResponse = withContext(Dispatchers.IO) {
        val account = repository.findById(id).orElseThrow()
        val endpoint = account.balanceEndpoint
        if (endpoint == null) {
            account.status = AccountStatus.UNSUPPORTED.name.lowercase()
            account.balanceCheckedAt = OffsetDateTime.now()
            return@withContext AccountBalanceResponse(
                accountId = id,
                balance = account.balance,
                currency = account.currency,
                status = AccountStatus.UNSUPPORTED,
                checkedAt = account.balanceCheckedAt.toString()
            )
        }

        val json = httpClient.getJson(endpoint, mapOf("Authorization" to "Bearer ${account.apiKey}"))
        val parsed = parseBalance(mapper.readTree(json))
        val status = if (parsed.first != null) AccountStatus.ACTIVE else AccountStatus.UNKNOWN
        account.balance = parsed.first
        account.currency = parsed.second
        account.status = status.name.lowercase()
        account.balanceCheckedAt = OffsetDateTime.now()
        AccountBalanceResponse(id, account.balance, account.currency, status, account.balanceCheckedAt.toString())
    }

    private fun validateEndpoint(url: String) {
        require(url.isNotBlank()) { "Protocol endpoint URL must not be blank" }
        val uri = runCatching { URI(url) }.getOrElse { throw IllegalArgumentException("Invalid protocol endpoint URL: $url") }
        require(uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) { "Protocol endpoint must use http or https" }
        require(!uri.host.isNullOrBlank()) { "Protocol endpoint must contain a host" }
        require(!uri.path.isNullOrBlank()) { "Protocol endpoint must be a complete endpoint URL" }
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
