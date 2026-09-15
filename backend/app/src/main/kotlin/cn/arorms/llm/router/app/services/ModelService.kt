package cn.arorms.llm.router.app.services

import cn.arorms.llm.router.app.entities.ModelCatalog
import cn.arorms.llm.router.app.repositories.ModelCatalogRepository
import cn.arorms.llm.router.app.repositories.ProviderAccountRepository
import cn.arorms.llm.router.common.enums.Protocol
import cn.arorms.llm.router.common.requests.ManualModelRequest
import cn.arorms.llm.router.common.responses.ModelListResponse
import cn.arorms.llm.router.common.responses.ModelResponse
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ModelService(
    private val accountRepository: ProviderAccountRepository,
    private val catalogRepository: ModelCatalogRepository,
    private val httpClient: GatewayHttpClient,
    private val mapper: ObjectMapper
) {
    @Transactional
    fun localModels(): ModelListResponse {
        val enabledAccountIds = accountRepository.findAll().filter { it.enabled }.mapNotNull { it.id }.toSet()
        val grouped = catalogRepository.findByEnabledTrue()
            .filter { it.accountId in enabledAccountIds }
            .groupBy { it.publicName ?: "${it.provider}/${it.modelId}" }
        return ModelListResponse(
            provider = "llm-router",
            data = grouped.map { (_, deployments) ->
                deployments.first().toResponse(protocols = deployments.map { it.protocol }.toSet())
            }
        )
    }

    @Transactional
    fun create(request: ManualModelRequest): ModelResponse {
        val account = accountRepository.findById(request.accountId).orElseThrow()
        require(request.model.isNotBlank() && !request.model.contains('/')) { "Model name must not be blank or contain '/'" }
        require(!catalogRepository.existsByAccountIdAndModelIdAndProtocol(account.id ?: 0L, request.model, request.protocol)) {
            "Model ${request.model} already exists for ${account.name}/${request.protocol}"
        }
        require(account.supports(request.protocol)) {
            "${account.name} does not support ${request.protocol}"
        }
        val protocol = request.protocol
        return catalogRepository.save(
            ModelCatalog(
                accountId = account.id ?: 0L,
                provider = account.name,
                protocol = protocol,
                modelId = request.model,
                publicName = "${account.name}/${request.model}",
                upstreamModel = request.upstreamModel ?: request.model,
                ownedBy = request.ownedBy ?: account.name,
                displayName = request.displayName,
                enabled = request.enabled,
                manual = true,
                maxContextTokens = request.maxContextTokens,
                maxOutputTokens = request.maxOutputTokens
            )
        ).toResponse()
    }

    @Transactional
    fun setEnabled(id: Long, enabled: Boolean): ModelResponse {
        val model = catalogRepository.findById(id).orElseThrow()
        model.enabled = enabled
        return model.toResponse()
    }

    @Transactional
    fun delete(id: Long) = catalogRepository.deleteById(id)

    suspend fun remoteModels(protocol: Protocol): ModelListResponse = withContext(Dispatchers.IO) {
        val results = accountRepository.findByEnabledTrueOrderByPriorityAscIdAsc()
            .filter { it.supports(protocol) }
            .flatMap { account ->
                account.configuration["modelsEndpoint"]?.let { endpoint ->
                    fetchUpstreamModels(account.id, account.name, protocol, endpoint, account.apiKey)
                } ?: emptyList()
            }
        ModelListResponse(protocol.name.lowercase(), data = results.distinctBy(ModelResponse::id))
    }

    suspend fun sync(accountId: Long): ModelListResponse {
        val account = accountRepository.findById(accountId).orElseThrow()
        return sync(accountId, account.defaultProtocol)
    }

    suspend fun sync(accountId: Long, protocol: Protocol): ModelListResponse = withContext(Dispatchers.IO) {
        val account = accountRepository.findById(accountId).orElseThrow()
        require(account.supports(protocol)) { "${account.name} does not support $protocol" }
        val modelsEndpoint = account.configuration["modelsEndpoint"]
            ?: throw IllegalArgumentException("Configure modelsEndpoint before syncing ${account.name}")
        val upstreamModels = fetchUpstreamModels(account.id, account.name, protocol, modelsEndpoint, account.apiKey)
        val existing = catalogRepository.findByAccountIdAndProtocol(accountId, protocol)
        val existingByModel = existing.associateBy { it.modelId }
        val synced = upstreamModels.map { response ->
            val current = existingByModel[response.model]
            current?.apply {
                this.ownedBy = response.ownedBy
            } ?: ModelCatalog(
                accountId = accountId,
                provider = account.name,
                protocol = protocol,
                modelId = response.model,
                publicName = "${account.name}/${response.model}",
                upstreamModel = response.model,
                ownedBy = response.ownedBy,
                displayName = response.displayName,
                enabled = true,
                manual = false
            )
        }
        val manualModels = existing.filter { it.manual && it.modelId !in synced.map(ModelCatalog::modelId) }
        catalogRepository.saveAll(synced + manualModels)
        ModelListResponse(protocol.name.lowercase(), data = (synced + manualModels).map { it.toResponse() })
    }

    private suspend fun fetchUpstreamModels(
        accountId: Long,
        provider: String,
        protocol: Protocol,
        modelsEndpoint: String,
        apiKey: String
    ): List<ModelResponse> {
        val (url, headers) = when (protocol) {
            Protocol.OPENAI, Protocol.OPENAI_RESPONSES -> modelsEndpoint to mapOf("Authorization" to "Bearer $apiKey")
            Protocol.ANTHROPIC -> modelsEndpoint to mapOf("x-api-key" to apiKey, "anthropic-version" to "2023-06-01")
        }
        val body = mapper.readTree(httpClient.getJson(url, headers))
        return body.path("data").map {
            val modelId = it.path("id").asText()
            ModelResponse(
                id = "$provider/$modelId",
                provider = provider,
                model = modelId,
                protocol = protocol,
                upstreamModel = modelId,
                ownedBy = it.path("owned_by").asText(it.path("ownedBy").asText(provider))
            )
        }
    }

    private fun ModelCatalog.toResponse(protocols: Set<cn.arorms.llm.router.common.enums.Protocol> = setOf(protocol)) = ModelResponse(
        id = publicName ?: "$provider/$modelId",
        provider = provider,
        model = modelId,
        protocol = protocol,
        protocols = protocols,
        ownedBy = ownedBy ?: provider,
        displayName = displayName,
        upstreamModel = upstreamModel ?: modelId,
        enabled = enabled,
        maxContextTokens = maxContextTokens,
        maxOutputTokens = maxOutputTokens
    )
}
