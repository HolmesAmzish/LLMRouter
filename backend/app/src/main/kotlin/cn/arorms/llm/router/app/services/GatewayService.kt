package cn.arorms.llm.router.app.services

import cn.arorms.llm.router.app.adapters.Adapters
import cn.arorms.llm.router.app.entities.ProviderAccount
import cn.arorms.llm.router.app.repositories.ProviderAccountRepository
import cn.arorms.llm.router.app.repositories.SessionRepository
import cn.arorms.llm.router.common.responses.ChatResponse
import cn.arorms.llm.router.common.enums.Protocol
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus

@Service
class GatewayService(
    private val accountRepository: ProviderAccountRepository,
    private val sessionRepository: SessionRepository,
    private val usageRecorder: UsageRecorder,
    private val cacheService: CacheService,
    private val httpClient: GatewayHttpClient,
    private val mapper: ObjectMapper
) {
    suspend fun chat(body: JsonNode, inboundProtocol: Protocol): JsonNode = withContext(Dispatchers.IO) {
        val request = Adapters.parseRequest(body, inboundProtocol)
        if (request.stream) throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Streaming is not enabled in the initial router release")
        val account = selectAccount(inboundProtocol)
        val outboundModel = account.modelMapping[request.model] ?: request.model
        val outboundRequest = Adapters.toOutboundRequest(request.copy(model = outboundModel), account.protocol)
        val fingerprint = CacheService.fingerprint(inboundProtocol.name, request.model, request.thinkingEffort.name, outboundRequest.toString())

        val startedAt = System.nanoTime()
        val cache = cacheService.get(fingerprint)
        val response = if (cache != null) {
            mapper.readTree(cache)
        } else {
            val url = upstreamUrl(account, outboundModel)
            val payload = mapper.writeValueAsString(outboundRequest)
            val raw = httpClient.postJson(url, headers(account), payload)
            val parsed = mapper.readTree(raw)
            val canonical = Adapters.parseResponse(parsed, request.copy(model = outboundModel), account.protocol.name, account.name)
            cacheService.put(fingerprint, mapper.writeValueAsString(Adapters.toInboundResponse(canonical, inboundProtocol)), request.sessionId, inboundProtocol, request.model)
            Adapters.toInboundResponse(canonical, inboundProtocol).also {
                usageRecorder.record(request.sessionId, account, request.model, canonical, startedAt, "success")
                usageRecorder.updateSession(request.sessionId, canonical)
            }
        }
        response
    }

    private fun selectAccount(protocol: Protocol): ProviderAccount {
        val accounts = accountRepository.findByEnabledTrueOrderByPriorityAscIdAsc().filter { it.protocol == protocol }
        if (accounts.isEmpty()) throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No enabled ${protocol.name} account")
        val lowestPriority = accounts.minOf { it.priority }
        val candidates = accounts.filter { it.priority == lowestPriority }
        if (candidates.size == 1) return candidates.first()
        val totalWeight = candidates.sumOf { it.weight }.coerceAtLeast(1)
        var chosen = kotlin.random.Random.nextInt(totalWeight)
        candidates.forEach { account ->
            chosen -= account.weight
            if (chosen < 0) return account
        }
        return candidates.first()
    }

    private fun upstreamUrl(account: ProviderAccount, model: String): String = when (account.protocol) {
        Protocol.OPENAI -> "${account.baseUrl}/v1/chat/completions"
        Protocol.ANTHROPIC -> "${account.baseUrl}/v1/messages"
        Protocol.GEMINI -> "${account.baseUrl}/v1beta/models/$model:generateContent"
    }

    private fun headers(account: ProviderAccount): Map<String, String> = when (account.protocol) {
        Protocol.OPENAI -> mapOf("Authorization" to "Bearer ${account.apiKey}")
        Protocol.ANTHROPIC -> mapOf("x-api-key" to account.apiKey, "anthropic-version" to "2023-06-01")
        Protocol.GEMINI -> mapOf("x-goog-api-key" to account.apiKey)
    }
}
