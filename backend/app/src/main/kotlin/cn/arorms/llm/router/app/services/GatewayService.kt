package cn.arorms.llm.router.app.services

import cn.arorms.llm.router.app.adapters.Adapters
import cn.arorms.llm.router.app.adapters.InboundStreamState
import cn.arorms.llm.router.app.adapters.SseFrame
import cn.arorms.llm.router.app.adapters.StreamAdapters
import cn.arorms.llm.router.app.entities.ProviderAccount
import cn.arorms.llm.router.app.repositories.ProviderAccountRepository
import cn.arorms.llm.router.app.repositories.SessionRepository
import cn.arorms.llm.router.common.responses.ChatResponse
import cn.arorms.llm.router.common.responses.ChatStreamChunk
import cn.arorms.llm.router.common.responses.Usage
import cn.arorms.llm.router.common.enums.Protocol
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.flux
import kotlinx.coroutines.withContext
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus

@Service
class GatewayService(
    private val accountRepository: ProviderAccountRepository,
    private val sessionRepository: SessionRepository,
    private val usageRecorder: UsageRecorder,
    private val cacheService: CacheService,
    private val httpClient: GatewayHttpClient,
    private val streamingClient: GatewayStreamingClient,
    private val mapper: ObjectMapper
) {
    suspend fun chat(body: JsonNode, inboundProtocol: Protocol): JsonNode = withContext(Dispatchers.IO) {
        val request = Adapters.parseRequest(body, inboundProtocol)
        if (request.stream) throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Streaming is not enabled in the initial router release")
        val account = selectAccount(inboundProtocol)
        val upstreamBaseUrl = account.endpointFor(inboundProtocol)
            ?: throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No ${inboundProtocol.name} endpoint for ${account.name}")
        val outboundModel = account.modelMapping[request.model] ?: request.model
        val outboundRequest = Adapters.toOutboundRequest(request.copy(model = outboundModel), inboundProtocol)
        val fingerprint = CacheService.fingerprint(inboundProtocol.name, request.model, request.thinkingEffort.name, outboundRequest.toString())

        val startedAt = System.nanoTime()
        val cache = cacheService.get(fingerprint)
        val response = if (cache != null) {
            mapper.readTree(cache)
        } else {
            val url = upstreamUrl(inboundProtocol, upstreamBaseUrl, outboundModel)
            val payload = mapper.writeValueAsString(outboundRequest)
            val raw = httpClient.postJson(url, headers(inboundProtocol, account.apiKey), payload)
            val parsed = mapper.readTree(raw)
            val canonical = Adapters.parseResponse(parsed, request.copy(model = outboundModel), inboundProtocol.name, account.name)
            cacheService.put(fingerprint, mapper.writeValueAsString(Adapters.toInboundResponse(canonical, inboundProtocol)), request.sessionId, inboundProtocol, request.model)
            Adapters.toInboundResponse(canonical, inboundProtocol).also {
                usageRecorder.record(request.sessionId, account, request.model, inboundProtocol, canonical, startedAt, "success")
                usageRecorder.updateSession(request.sessionId, canonical)
            }
        }
        response
    }


    fun chatStream(body: JsonNode, inboundProtocol: Protocol): Flux<ServerSentEvent<String>> = flux(Dispatchers.IO) {
        val request = Adapters.parseRequest(body, inboundProtocol).copy(stream = true)
        val account = selectAccount(inboundProtocol)
        val upstreamBaseUrl = account.endpointFor(inboundProtocol)
            ?: throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No ${inboundProtocol.name} endpoint for ${account.name}")
        val outboundModel = account.modelMapping[request.model] ?: request.model
        val outboundRequest = Adapters.toOutboundRequest(request.copy(model = outboundModel), inboundProtocol)
        val startedAt = System.nanoTime()
        val state = InboundStreamState(requestedModel = request.model)
        val chunks = mutableListOf<ChatStreamChunk>()
        val passthrough = request.stream

        try {
            streamingClient
                .stream(streamingUrl(inboundProtocol, upstreamBaseUrl, outboundModel), headers(inboundProtocol, account.apiKey), mapper.writeValueAsString(outboundRequest))
                .asFlow()
                .collect { event: ServerSentEvent<String> ->
                    val upstreamData = event.data()
                    val chunk = StreamAdapters.parseUpstreamEvent(upstreamData, inboundProtocol)
                    chunk?.let(chunks::add)

                    val frames: List<SseFrame> = if (passthrough) {
                        listOf(SseFrame(event = event.event(), data = upstreamData ?: ""))
                    } else {
                        StreamAdapters.toInboundFrames(chunk, inboundProtocol, state)
                    }

                    frames.forEach { frame ->
                        if (frame.data.isNotBlank()) {
                            val builder = ServerSentEvent.builder<String>(frame.data)
                            frame.event?.takeIf { it.isNotBlank() }?.let(builder::event)
                            send(builder.build())
                        }
                    }
                }

            if (!passthrough) {
                StreamAdapters.doneFrame(inboundProtocol)?.let { frame ->
                    send(ServerSentEvent.builder<String>(frame.data).build())
                }
            }

            val usage = Usage(
                inputTokens = chunks.maxOfOrNull { it.usage?.inputTokens ?: 0L } ?: 0L,
                outputTokens = chunks.lastOrNull { it.usage?.outputTokens != null }?.usage?.outputTokens
                    ?: chunks.sumOf { it.usage?.outputTokens ?: 0L },
                costCents = null
            )
            val response = ChatResponse(
                id = chunks.firstOrNull { it.id.isNotBlank() }?.id ?: state.fallbackId(),
                model = chunks.firstOrNull { it.model.isNotBlank() }?.model ?: request.model,
                choices = emptyList(),
                usage = usage,
                provider = inboundProtocol.name,
                accountName = account.name,
                createdAt = startedAt / 1_000_000_000
            )
            usageRecorder.record(request.sessionId, account, request.model, inboundProtocol, response, startedAt, "success")
            usageRecorder.updateSession(request.sessionId, response)
        } catch (cause: Throwable) {
            usageRecorder.record(
                sessionId = request.sessionId,
                account = account,
                requestedModel = request.model,
                protocol = inboundProtocol,
                response = ChatResponse(
                    id = state.fallbackId(),
                    model = request.model,
                    choices = emptyList(),
                    usage = null,
                    provider = inboundProtocol.name,
                    accountName = account.name,
                    createdAt = System.nanoTime() / 1_000_000_000
                ),
                startedAt = startedAt,
                status = "failed"
            )
            throw cause
        }
    }

    private fun selectAccount(protocol: Protocol): ProviderAccount {
        val accounts = accountRepository.findByEnabledTrueOrderByPriorityAscIdAsc().filter { it.supports(protocol) }
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

    private fun upstreamUrl(protocol: Protocol, baseUrl: String, model: String): String = when (protocol) {
        Protocol.OPENAI -> "$baseUrl/v1/chat/completions"
        Protocol.OPENAI_RESPONSES -> "$baseUrl/v1/responses"
        Protocol.ANTHROPIC -> "$baseUrl/v1/messages"
    }

    private fun streamingUrl(protocol: Protocol, baseUrl: String, model: String): String = when (protocol) {
        Protocol.OPENAI -> "$baseUrl/v1/chat/completions"
        Protocol.OPENAI_RESPONSES -> "$baseUrl/v1/responses"
        Protocol.ANTHROPIC -> "$baseUrl/v1/messages"
    }

    private fun headers(protocol: Protocol, apiKey: String): Map<String, String> = when (protocol) {
        Protocol.OPENAI, Protocol.OPENAI_RESPONSES -> mapOf("Authorization" to "Bearer $apiKey")
        Protocol.ANTHROPIC -> mapOf("x-api-key" to apiKey, "anthropic-version" to "2023-06-01")
    }
}
