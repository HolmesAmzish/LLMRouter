package cn.arorms.llm.router.app.services

import cn.arorms.llm.router.app.entities.Model
import cn.arorms.llm.router.app.entities.ProviderAccount
import cn.arorms.llm.router.app.entities.ProviderModel
import cn.arorms.llm.router.app.repositories.ModelRepository
import cn.arorms.llm.router.app.repositories.ProviderAccountRepository
import cn.arorms.llm.router.app.repositories.ProviderModelRepository
import cn.arorms.llm.router.common.enums.Protocol
import cn.arorms.llm.router.common.requests.ModelPricePatch
import cn.arorms.llm.router.common.requests.ModelPriceRequest
import cn.arorms.llm.router.common.requests.ProviderModelRequest
import cn.arorms.llm.router.common.responses.ModelListResponse
import cn.arorms.llm.router.common.responses.ModelPriceResponse
import cn.arorms.llm.router.common.responses.ModelResponse
import cn.arorms.llm.router.common.responses.ProviderModelResponse
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ModelService(
    private val modelRepository: ModelRepository,
    private val providerModelRepository: ProviderModelRepository,
    private val accountRepository: ProviderAccountRepository,
    private val httpClient: GatewayHttpClient,
    private val mapper: ObjectMapper
) {
    @Transactional
    fun listPrices(): List<ModelPriceResponse> =
        modelRepository.findAllByOrderByIdDesc().map { it.toResponse() }

    @Transactional
    fun createPrice(request: ModelPriceRequest): ModelPriceResponse {
        val modelId = request.modelId.trim()
        require(modelId.isNotBlank()) { "Model id must not be blank" }
        require(!modelRepository.existsByModelId(modelId)) { "Model $modelId already exists" }
        val model = modelRepository.save(
            Model(
                modelId = modelId,
                modelName = request.modelName.trim().ifBlank { modelId },
                ownedBy = request.ownedBy?.trim()?.takeIf { it.isNotEmpty() },
                enabled = request.enabled,
                inputCostPerMillion = request.inputCostPerMillion,
                outputCostPerMillion = request.outputCostPerMillion,
                cacheReadCostPerMillion = request.cacheReadCostPerMillion,
                cacheCreationCostPerMillion = request.cacheCreationCostPerMillion,
                currency = request.currency.trim().ifBlank { "USD" }.uppercase()
            )
        )
        linkProviderModels(model)
        return model.toResponse()
    }

    @Transactional
    fun updatePrice(id: Long, patch: ModelPricePatch): ModelPriceResponse {
        val model = modelRepository.findById(id).orElseThrow()
        patch.modelName?.takeIf { it.isNotBlank() }?.let { model.modelName = it.trim() }
        patch.ownedBy?.let { model.ownedBy = it.trim().takeIf { value -> value.isNotEmpty() } }
        patch.enabled?.let { model.enabled = it }
        patch.inputCostPerMillion?.let { model.inputCostPerMillion = it }
        patch.outputCostPerMillion?.let { model.outputCostPerMillion = it }
        patch.cacheReadCostPerMillion?.let { model.cacheReadCostPerMillion = it }
        patch.cacheCreationCostPerMillion?.let { model.cacheCreationCostPerMillion = it }
        patch.currency?.takeIf { it.isNotBlank() }?.let { model.currency = it.trim().uppercase() }
        val saved = modelRepository.save(model)
        linkProviderModels(saved)
        return saved.toResponse()
    }

    @Transactional
    fun deletePrice(id: Long) {
        providerModelRepository.clearModelDefinition(id)
        modelRepository.deleteById(id)
    }

    @Transactional
    fun replaceProviderModels(providerId: Long, providerName: String, requests: List<ProviderModelRequest>) {
        val normalized = requests.map { it.copy(modelId = it.modelId.trim(), modelName = it.modelName?.trim()?.ifBlank { null }) }
        val duplicate = normalized.groupBy { it.modelId }.filterKeys { it.isNotBlank() }.any { it.value.size > 1 }
        require(!duplicate) { "Model ids must be unique within a provider" }
        providerModelRepository.deleteByProviderId(providerId)
        val models = requests.mapNotNull { request ->
            val modelId = request.modelId.trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
            ProviderModel(
                providerId = providerId,
                providerName = providerName,
                modelId = modelId,
                modelName = request.modelName?.trim()?.ifBlank { null } ?: modelId,
                modelDefinitionId = resolveModelDefinitionId(request.modelPriceId, modelId),
                enabled = request.enabled
            )
        }
        providerModelRepository.saveAll(models)
    }

    @Transactional
    fun setProviderModelEnabled(id: Long, enabled: Boolean): ProviderModelResponse {
        val binding = providerModelRepository.findById(id).orElseThrow()
        binding.enabled = enabled
        return providerModelRepository.save(binding).toResponse()
    }

    @Transactional
    fun deleteProviderModel(id: Long) = providerModelRepository.deleteById(id)

    @Transactional
    fun localModels(): ModelListResponse {
        val accountsById = accountRepository.findAll().associateBy { it.id }
        val bindings = providerModelRepository.findByEnabledTrue()
            .filter { accountsById[it.providerId]?.enabled == true }
            .sortedBy { "${it.providerName}/${it.modelId}" }
        return ModelListResponse(
            provider = "llm-router",
            data = bindings.map { binding -> binding.toGatewayModel(accountsById.getValue(binding.providerId)) }
        )
    }

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
        fetchUpstreamModels(accountId, account.name, protocol, modelsEndpoint, account.apiKey).forEach { response ->
            val current = providerModelRepository.findByProviderIdAndModelId(accountId, response.model)
            if (current == null) {
                providerModelRepository.save(
                    ProviderModel(
                        providerId = accountId,
                        providerName = account.name,
                        modelId = response.model,
                        modelName = response.modelName,
                        modelDefinitionId = modelRepository.findByModelId(response.model)?.id,
                        enabled = true
                    )
                )
            } else {
                current.providerName = account.name
                current.modelName = response.modelName
                current.modelDefinitionId = current.modelDefinitionId
                    ?: modelRepository.findByModelId(response.model)?.id
                providerModelRepository.save(current)
            }
        }
        ModelListResponse(
            protocol.name.lowercase(),
            data = providerModelRepository.findByProviderIdOrderByModelIdAsc(accountId)
                .map { binding -> binding.toGatewayModel(account) }
        )
    }

    private fun linkProviderModels(model: Model) {
        providerModelRepository.findByModelId(model.modelId).forEach { binding ->
            binding.modelDefinitionId = model.id
            providerModelRepository.save(binding)
        }
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
                modelName = it.path("name").asText(it.path("display_name").asText(modelId)),
                upstreamModel = modelId,
                ownedBy = it.path("owned_by").asText(it.path("ownedBy").asText(provider))
            )
        }
    }

    private fun resolveModelDefinitionId(modelPriceId: Long?, modelId: String): Long? {
        modelPriceId?.let { explicit ->
            modelRepository.findById(explicit).orElse(null)?.let { model ->
                if (model.modelId == modelId) return model.id
            }
        }
        return modelRepository.findByModelId(modelId)?.id
    }

    private fun ProviderModel.toGatewayModel(account: ProviderAccount) = ModelResponse(
        id = "$providerName/$modelId",
        provider = providerName,
        model = modelId,
        protocol = account.defaultProtocol,
        protocols = account.protocolEndpoints.keys.mapNotNull {
            runCatching { Protocol.valueOf(it) }.getOrNull()
        }.toSet(),
        ownedBy = modelRepository.findByModelId(modelId)?.ownedBy ?: providerName,
        modelName = modelName,
        displayName = modelRepository.findByModelId(modelId)?.modelName,
        upstreamModel = account.modelMapping[modelId] ?: modelId,
        enabled = enabled
    )

    private fun ProviderModel.toResponse() = ProviderModelResponse(
        id = id ?: 0L,
        providerId = providerId,
        providerName = providerName,
        modelId = modelId,
        modelName = modelName,
        modelPriceId = modelDefinitionId,
        priced = modelDefinitionId != null,
        enabled = enabled
    )

    private fun Model.toResponse() = ModelPriceResponse(
        id = id ?: 0L,
        modelId = modelId,
        modelName = modelName,
        ownedBy = ownedBy,
        enabled = enabled,
        inputCostPerMillion = inputCostPerMillion,
        outputCostPerMillion = outputCostPerMillion,
        cacheReadCostPerMillion = cacheReadCostPerMillion,
        cacheCreationCostPerMillion = cacheCreationCostPerMillion,
        currency = currency,
        createdAt = createdAt?.toString(),
        updatedAt = updatedAt?.toString()
    )
}
