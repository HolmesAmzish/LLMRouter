package cn.arorms.llm.router.app.services

import cn.arorms.llm.router.app.entities.ModelCatalog
import cn.arorms.llm.router.app.repositories.ModelCatalogRepository
import cn.arorms.llm.router.app.repositories.ProviderAccountRepository
import cn.arorms.llm.router.common.responses.ModelListResponse
import cn.arorms.llm.router.common.responses.ModelResponse
import cn.arorms.llm.router.common.enums.Protocol
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
    fun localModels(): ModelListResponse = ModelListResponse(
        provider = "llm-router",
        data = catalogRepository.findByEnabledTrue().map { ModelResponse(id = it.modelId, ownedBy = it.ownedBy ?: "llm-router", enabled = it.enabled) }
            .distinctBy(ModelResponse::id)
    )

    suspend fun remoteModels(protocol: Protocol): ModelListResponse = withContext(Dispatchers.IO) {
        val results = accountRepository.findByEnabledTrueOrderByPriorityAscIdAsc()
            .filter { it.protocol == protocol }
            .flatMap { account -> fetchModels(account.id, account.protocol, account.baseUrl, account.apiKey) }
        ModelListResponse(protocol.name.lowercase(), data = results.distinctBy(ModelResponse::id))
    }

    suspend fun sync(accountId: Long): ModelListResponse = withContext(Dispatchers.IO) {
        val account = accountRepository.findById(accountId).orElseThrow()
        val models = fetchModels(account.id, account.protocol, account.baseUrl, account.apiKey)
        catalogRepository.deleteByAccountId(account.id)
        catalogRepository.saveAll(models.map { ModelCatalog(account.id, it.id, it.ownedBy, true) })
        ModelListResponse(account.protocol.name.lowercase(), data = models)
    }

    private suspend fun fetchModels(accountId: Long, protocol: Protocol, baseUrl: String, apiKey: String): List<ModelResponse> {
        val (url, headers) = when (protocol) {
            Protocol.OPENAI -> "$baseUrl/v1/models" to mapOf("Authorization" to "Bearer $apiKey")
            Protocol.ANTHROPIC -> "$baseUrl/v1/models?limit=1000" to mapOf("x-api-key" to apiKey, "anthropic-version" to "2023-06-01")
            Protocol.GEMINI -> "$baseUrl/v1beta/models?pageSize=1000" to mapOf("x-goog-api-key" to apiKey)
        }
        val body = mapper.readTree(httpClient.getJson(url, headers))
        return when (protocol) {
            Protocol.OPENAI, Protocol.ANTHROPIC -> body.path("data").map { ModelResponse(id = it.path("id").asText(), ownedBy = it.path("owned_by").asText(it.path("ownedBy").asText(protocol.name.lowercase()))) }
            Protocol.GEMINI -> body.path("models").map { ModelResponse(id = it.path("name").asText().removePrefix("models/"), ownedBy = "google", enabled = it.path("supportedGenerationMethods").any { method -> method.asText() == "generateContent" }) }
        }
    }
}
