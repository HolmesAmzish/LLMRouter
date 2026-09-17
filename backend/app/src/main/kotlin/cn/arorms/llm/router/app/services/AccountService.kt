package cn.arorms.llm.router.app.services

import cn.arorms.llm.router.app.entities.ProviderAccount
import cn.arorms.llm.router.app.mappers.ProviderAccountMapper
import cn.arorms.llm.router.app.repositories.ProviderModelRepository
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
    private val providerModelRepository: ProviderModelRepository,
    private val modelService: ModelService,
    private val httpClient: GatewayHttpClient,
    private val mapper: ObjectMapper
) {
    @Transactional
    fun create(request: ProviderAccountRequest): ProviderAccountResponse {
        require(request.protocolEndpoints.isNotEmpty()) {
            "At least one protocol endpoint is required"
        }

        val endpoints = request.protocolEndpoints.mapValues { (_, url) -> url.trim() }
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
        modelService.replaceProviderModels(
            providerId = account.id ?: 0L,
            providerName = account.name,
            requests = request.models
        )
        return toResponse(account)
    }

    @Transactional
    fun update(id: Long, patch: ProviderAccountPatch): ProviderAccountResponse {
        val account = repository.findById(id).orElseThrow()
        val oldName = account.name
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
        val saved = repository.save(account)

        val requestedModels = patch.models
        if (requestedModels != null) {
            modelService.replaceProviderModels(saved.id ?: 0L, saved.name, requestedModels)
        } else if (saved.name != oldName) {
            providerModelRepository.findByProviderIdOrderByModelIdAsc(saved.id ?: 0L).forEach { binding ->
                binding.providerName = saved.name
                providerModelRepository.save(binding)
            }
        }
        return toResponse(saved)
    }

    @Transactional
    fun delete(id: Long) {
        providerModelRepository.deleteByProviderId(id)
        repository.deleteById(id)
    }

    @Transactional
    fun get(id: Long): ProviderAccountResponse =
        repository.findById(id).orElseThrow().let { toResponse(it) }

    @Transactional
    fun list(provider: String?, enabled: Boolean? = null): List<ProviderAccountResponse> {
        val all = repository.findAll().sortedBy { it.priority }
        return all.asSequence()
            .filter { provider == null || Protocol.entries.any { protocol -> protocol.name.equals(provider, true) && it.supports(protocol) } }
            .filter { enabled == null || it.enabled == enabled }
            .map(::toResponse)
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
                checkedAt = account.balanceCheckedAt?.toString() ?: OffsetDateTime.now().toString()
            )
        }

        val raw = httpClient.getJson(endpoint, headers(account.apiKey))
        val body: JsonNode = mapper.readTree(raw)
        val parsed = listOf(
            body.path("balance"),
            body.path("data").path("balance"),
            body.path("data").path("credit"),
            body.path("credit")
        ).firstOrNull { it.isNumber }?.asDouble()

        parsed?.let {
            account.balance = it
            account.status = AccountStatus.ACTIVE.name.lowercase()
        } ?: run {
            account.status = AccountStatus.UNKNOWN.name.lowercase()
        }
        account.balanceCheckedAt = OffsetDateTime.now()
        AccountBalanceResponse(
            accountId = id,
            balance = account.balance,
            currency = account.currency,
            status = account.status.uppercase().let {
                runCatching { AccountStatus.valueOf(it) }.getOrDefault(AccountStatus.UNKNOWN)
            },
            checkedAt = account.balanceCheckedAt?.toString() ?: OffsetDateTime.now().toString()
        )
    }

    private fun toResponse(account: ProviderAccount): ProviderAccountResponse {
        val models = providerModelRepository.findByProviderIdOrderByModelIdAsc(account.id ?: 0L)
        return ProviderAccountMapper.toResponse(account, models)
    }

    private fun headers(apiKey: String): Map<String, String> = mapOf("Authorization" to "Bearer $apiKey")

    private fun validateEndpoint(value: String) {
        val uri = runCatching { URI(value) }.getOrElse { throw IllegalArgumentException("Invalid endpoint URL: $value") }
        require(uri.scheme == "http" || uri.scheme == "https") { "Endpoint URL must use http or https" }
        require(!uri.host.isNullOrBlank()) { "Endpoint URL must include a host" }
    }
}
