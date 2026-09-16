package cn.arorms.llm.router.app.services

import cn.arorms.llm.router.app.adapters.Adapters
import cn.arorms.llm.router.app.adapters.InboundStreamState
import cn.arorms.llm.router.app.adapters.SseFrame
import cn.arorms.llm.router.app.adapters.StreamAdapters
import cn.arorms.llm.router.app.entities.ApiKey
import cn.arorms.llm.router.app.entities.ProviderModel
import cn.arorms.llm.router.app.entities.ProviderAccount
import cn.arorms.llm.router.app.repositories.ProviderModelRepository
import cn.arorms.llm.router.app.repositories.ProviderAccountRepository
import cn.arorms.llm.router.common.enums.Protocol
import cn.arorms.llm.router.common.enums.UsageDataSource
import cn.arorms.llm.router.common.responses.ChatResponse
import cn.arorms.llm.router.common.responses.ChatStreamChunk
import cn.arorms.llm.router.common.responses.Usage
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.flux
import kotlinx.coroutines.withContext
import org.springframework.http.HttpStatus
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux

private data class ResolvedModel(
    val binding: ProviderModel,
    val account: ProviderAccount,
    val provider: String,
    val model: String,
    val qualifiedModel: String
) {
    val upstreamModel: String
        get() = account.modelMapping[model] ?: model
}

@Service
class GatewayService(
    private val accountRepository: ProviderAccountRepository,
    private val providerModelRepository: ProviderModelRepository,
    private val usageRecorder: UsageRecorder,
    private val cacheService: CacheService,
    private val httpClient: GatewayHttpClient,
    private val streamingClient: GatewayStreamingClient,
    private val mapper: ObjectMapper
) {
    suspend fun chat(rawBody: String, inboundProtocol: Protocol, apiKey: ApiKey? = null): String = withContext(Dispatchers.IO) {
        val body = mapper.readTree(rawBody)
        val request = Adapters.parseRequest(body, inboundProtocol)
        if (request.stream) throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Use the stream-capable gateway route for stream:true")
        val resolved = resolveModel(request.model, inboundProtocol, apiKey)
        val upstreamProtocol = inboundProtocol
        val upstreamEndpoint = resolved.account.endpointFor(upstreamProtocol)
            ?: throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No ${upstreamProtocol.name} endpoint for ${resolved.provider}")
        val upstreamModel = resolved.upstreamModel
        val outboundRequest = Adapters.toOutboundRequest(request.copy(model = upstreamModel), upstreamProtocol)
        val fingerprint = CacheService.fingerprint(upstreamProtocol.name, resolved.qualifiedModel, request.thinkingEffort.name, outboundRequest.toString())

        val startedAt = System.nanoTime()
        val cache = cacheService.get(fingerprint)
        if (cache != null) {
            val cachedBody = mapper.readTree(cache)
            val cachedResponse = Adapters.parseResponse(
                cachedBody,
                request.copy(model = upstreamModel),
                inboundProtocol.name,
                resolved.account.name
            )
            usageRecorder.record(
                sessionId = request.sessionId,
                account = resolved.account,
                providerName = resolved.provider,
                requestedModel = resolved.model,
                publicModel = resolved.qualifiedModel,
                upstreamModel = upstreamModel,
                protocol = upstreamProtocol,
                response = cachedResponse,
                startedAt = startedAt,
                status = "success",
                apiKey = apiKey,
                apiBase = upstreamEndpoint,
                cacheHit = true,
                cacheKey = fingerprint,
                dataSource = UsageDataSource.ROUTER_CACHE
            )
            cache
        } else {
            val url = upstreamUrl(upstreamEndpoint)
            val payload = mapper.writeValueAsString(outboundRequest)
            val raw = httpClient.postJson(url, headers(upstreamProtocol, resolved.account.apiKey), payload)
            val parsed = mapper.readTree(raw)
            val canonical = Adapters.parseResponse(parsed, request.copy(model = upstreamModel), upstreamProtocol.name, resolved.account.name)
            val inbound = mapper.writeValueAsString(Adapters.toInboundResponse(canonical, inboundProtocol))
            cacheService.put(fingerprint, inbound, request.sessionId, upstreamProtocol, resolved.qualifiedModel)
            usageRecorder.record(
                sessionId = request.sessionId,
                account = resolved.account,
                providerName = resolved.provider,
                requestedModel = resolved.model,
                publicModel = resolved.qualifiedModel,
                upstreamModel = upstreamModel,
                protocol = upstreamProtocol,
                response = canonical,
                startedAt = startedAt,
                status = "success",
                apiKey = apiKey,
                apiBase = upstreamEndpoint,
                cacheKey = fingerprint
            )
            usageRecorder.updateSession(request.sessionId, canonical)
            inbound
        }
    }

    fun chatStream(rawBody: String, inboundProtocol: Protocol, apiKey: ApiKey? = null): Flux<ServerSentEvent<String>> = flux(Dispatchers.IO) {
        val body = mapper.readTree(rawBody)
        val request = Adapters.parseRequest(body, inboundProtocol).copy(stream = true)
        val resolved = resolveModel(request.model, inboundProtocol, apiKey)
        val upstreamProtocol = inboundProtocol
        val upstreamEndpoint = resolved.account.endpointFor(upstreamProtocol)
            ?: throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No ${upstreamProtocol.name} endpoint for ${resolved.provider}")
        if (upstreamProtocol != inboundProtocol) {
            throw ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Cross-protocol streaming is not supported for ${resolved.qualifiedModel} (${inboundProtocol})"
            )
        }
        val upstreamModel = resolved.upstreamModel
        val outboundRequest = Adapters.toOutboundRequest(request.copy(model = upstreamModel), upstreamProtocol)
        val startedAt = System.nanoTime()
        val state = InboundStreamState(requestedModel = resolved.qualifiedModel, requestedProtocol = inboundProtocol)
        val chunks = mutableListOf<ChatStreamChunk>()
        val buffer = StringBuilder()
        var sawDone = false

        try {
            streamingClient
                .stream(streamingUrl(upstreamEndpoint), headers(upstreamProtocol, resolved.account.apiKey), mapper.writeValueAsString(outboundRequest))
                .asFlow()
                .collect { raw ->
                    if (raw.contains("[DONE]")) sawDone = true
                    buffer.append(raw)
                    extractFrames(buffer, upstreamProtocol, chunks)?.forEach { frame ->
                        if (frame.data.isNotBlank()) {
                            val builder = ServerSentEvent.builder<String>(frame.data)
                            frame.event?.takeIf { it.isNotBlank() }?.let(builder::event)
                            send(builder.build())
                        }
                    }
                }

            if (!sawDone) {
                StreamAdapters.doneFrame(inboundProtocol)?.let { frame ->
                    send(ServerSentEvent.builder<String>(frame.data).build())
                }
            }

            val usages = chunks.mapNotNull { it.usage }
            val usage = Usage(
                inputTokens = usages.maxOfOrNull { it.inputTokens } ?: 0L,
                outputTokens = usages.lastOrNull { it.outputTokens > 0L }?.outputTokens
                    ?: usages.sumOf { it.outputTokens },
                totalTokens = usages.lastOrNull { it.totalTokens > 0L }?.totalTokens ?: 0L,
                cacheReadTokens = usages.maxOfOrNull { it.cacheReadTokens } ?: 0L,
                cacheCreationTokens = usages.maxOfOrNull { it.cacheCreationTokens } ?: 0L,
                reasoningTokens = usages.mapNotNull { it.reasoningTokens }.maxOrNull(),
                inputTokenSemantics = usages.lastOrNull {
                    it.inputTokenSemantics != cn.arorms.llm.router.common.enums.TokenInputSemantics.UNKNOWN
                }?.inputTokenSemantics ?: cn.arorms.llm.router.common.enums.TokenInputSemantics.UNKNOWN,
                tokenDetails = usages.flatMap { it.tokenDetails.entries }
                    .groupBy({ it.key }, { it.value })
                    .mapValues { (_, values) -> values.max() },
                costCents = null
            )
            val response = ChatResponse(
                id = chunks.firstOrNull { it.id.isNotBlank() }?.id ?: state.fallbackId(),
                model = chunks.firstOrNull { it.model.isNotBlank() }?.model ?: resolved.qualifiedModel,
                choices = emptyList(),
                usage = usage,
                provider = resolved.provider,
                accountName = resolved.account.name,
                createdAt = startedAt / 1_000_000_000
            )
            usageRecorder.record(
                sessionId = request.sessionId,
                account = resolved.account,
                providerName = resolved.provider,
                requestedModel = resolved.model,
                publicModel = resolved.qualifiedModel,
                upstreamModel = upstreamModel,
                protocol = upstreamProtocol,
                response = response,
                startedAt = startedAt,
                status = "success",
                apiKey = apiKey,
                apiBase = upstreamEndpoint,
                isStreaming = true
            )
            usageRecorder.updateSession(request.sessionId, response)
        } catch (cause: Throwable) {
            usageRecorder.record(
                sessionId = request.sessionId,
                account = resolved.account,
                providerName = resolved.provider,
                requestedModel = resolved.model,
                publicModel = resolved.qualifiedModel,
                upstreamModel = resolved.upstreamModel,
                protocol = inboundProtocol,
                response = ChatResponse(
                    id = state.fallbackId(),
                    model = resolved.qualifiedModel,
                    choices = emptyList(),
                    usage = null,
                    provider = resolved.provider,
                    accountName = resolved.account.name,
                    createdAt = System.nanoTime() / 1_000_000_000
                ),
                startedAt = startedAt,
                status = "failed",
                apiKey = apiKey,
                apiBase = upstreamEndpoint,
                isStreaming = true,
                errorMessage = cause.message
            )
            throw cause
        }
    }

    private fun extractFrames(
        buffer: StringBuilder,
        protocol: Protocol,
        chunks: MutableList<ChatStreamChunk>
    ): List<SseFrame>? {
        val marker = "\n\n"
        val text = buffer.toString()
        val last = text.lastIndexOf(marker)
        if (last < 0) return null
        val complete = text.substring(0, last)
        buffer.setLength(0)
        buffer.append(text.substring(last + marker.length))
        var eventName: String? = null
        val frames = mutableListOf<SseFrame>()
        complete.split(marker).forEach { frame ->
            var data: String? = null
            frame.lineSequence().forEach { line ->
                when {
                    line.startsWith("event:") -> eventName = line.substring(6).trim()
                    line.startsWith("data:") -> data = line.substring(5).trim()
                }
            }
            StreamAdapters.parseUpstreamEvent(data, protocol)?.let(chunks::add)
            if (data != null) frames += SseFrame(event = eventName, data = data)
        }
        return frames
    }

    private fun resolveModel(qualifiedModel: String, protocol: Protocol, apiKey: ApiKey? = null): ResolvedModel {
        val separator = qualifiedModel.indexOf('/')
        require(separator > 0 && separator < qualifiedModel.length - 1) {
            "model must use <provider>/<model> format"
        }
        val provider = qualifiedModel.substring(0, separator)
        val model = qualifiedModel.substring(separator + 1)
        val binding = providerModelRepository
            .findByProviderNameAndModelIdAndEnabledTrue(provider, model)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown model $qualifiedModel")
        val account = accountRepository.findById(binding.providerId).orElseThrow()
        require(account.enabled && account.supports(protocol)) {
            "Model $qualifiedModel is not available for $protocol"
        }
        apiKey?.models?.takeIf { it.isNotEmpty() }?.let { allowed ->
            if (qualifiedModel !in allowed) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "API key cannot access $qualifiedModel")
            }
        }
        return ResolvedModel(binding, account, binding.providerName, binding.modelId, qualifiedModel)
    }

    private fun upstreamUrl(endpoint: String): String = endpoint

    private fun streamingUrl(endpoint: String): String = endpoint

    private fun headers(protocol: Protocol, apiKey: String): Map<String, String> = when (protocol) {
        Protocol.OPENAI, Protocol.OPENAI_RESPONSES -> mapOf("Authorization" to "Bearer $apiKey")
        Protocol.ANTHROPIC -> mapOf("x-api-key" to apiKey, "anthropic-version" to "2023-06-01")
    }
}
