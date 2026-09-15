package cn.arorms.llm.router.app.services

import cn.arorms.llm.router.app.adapters.Adapters
import cn.arorms.llm.router.app.adapters.InboundStreamState
import cn.arorms.llm.router.app.adapters.SseFrame
import cn.arorms.llm.router.app.adapters.StreamAdapters
import cn.arorms.llm.router.app.entities.ProviderAccount
import cn.arorms.llm.router.app.repositories.ModelCatalogRepository
import cn.arorms.llm.router.app.repositories.ProviderAccountRepository
import cn.arorms.llm.router.common.enums.Protocol
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
    val account: ProviderAccount,
    val provider: String,
    val model: String,
    val qualifiedModel: String
)

@Service
class GatewayService(
    private val accountRepository: ProviderAccountRepository,
    private val modelRepository: ModelCatalogRepository,
    private val usageRecorder: UsageRecorder,
    private val cacheService: CacheService,
    private val httpClient: GatewayHttpClient,
    private val streamingClient: GatewayStreamingClient,
    private val mapper: ObjectMapper
) {
    suspend fun chat(rawBody: String, inboundProtocol: Protocol): String = withContext(Dispatchers.IO) {
        val body = mapper.readTree(rawBody)
        val request = Adapters.parseRequest(body, inboundProtocol)
        if (request.stream) throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Use the stream-capable gateway route for stream:true")
        val resolved = resolveModel(request.model, inboundProtocol)
        val upstreamEndpoint = resolved.account.endpointFor(inboundProtocol)
            ?: throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No ${inboundProtocol.name} endpoint for ${resolved.provider}")
        val upstreamModel = resolved.account.modelMapping[resolved.model] ?: resolved.model
        val outboundRequest = Adapters.toOutboundRequest(request.copy(model = upstreamModel), inboundProtocol)
        val fingerprint = CacheService.fingerprint(inboundProtocol.name, resolved.qualifiedModel, request.thinkingEffort.name, outboundRequest.toString())

        val startedAt = System.nanoTime()
        val cache = cacheService.get(fingerprint)
        if (cache != null) {
            cache
        } else {
            val url = upstreamUrl(upstreamEndpoint)
            val payload = mapper.writeValueAsString(outboundRequest)
            val raw = httpClient.postJson(url, headers(inboundProtocol, resolved.account.apiKey), payload)
            val parsed = mapper.readTree(raw)
            val canonical = Adapters.parseResponse(parsed, request.copy(model = upstreamModel), inboundProtocol.name, resolved.account.name)
            val inbound = mapper.writeValueAsString(Adapters.toInboundResponse(canonical, inboundProtocol))
            cacheService.put(fingerprint, inbound, request.sessionId, inboundProtocol, resolved.qualifiedModel)
            usageRecorder.record(request.sessionId, resolved.account, resolved.provider, resolved.model, inboundProtocol, canonical, startedAt, "success")
            usageRecorder.updateSession(request.sessionId, canonical)
            inbound
        }
    }

    fun chatStream(rawBody: String, inboundProtocol: Protocol): Flux<ServerSentEvent<String>> = flux(Dispatchers.IO) {
        val body = mapper.readTree(rawBody)
        val request = Adapters.parseRequest(body, inboundProtocol).copy(stream = true)
        val resolved = resolveModel(request.model, inboundProtocol)
        val upstreamEndpoint = resolved.account.endpointFor(inboundProtocol)
            ?: throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No ${inboundProtocol.name} endpoint for ${resolved.provider}")
        val upstreamModel = resolved.account.modelMapping[resolved.model] ?: resolved.model
        val outboundRequest = Adapters.toOutboundRequest(request.copy(model = upstreamModel), inboundProtocol)
        val startedAt = System.nanoTime()
        val state = InboundStreamState(requestedModel = resolved.qualifiedModel, requestedProtocol = inboundProtocol)
        val chunks = mutableListOf<ChatStreamChunk>()
        val buffer = StringBuilder()
        var sawDone = false

        try {
            streamingClient
                .stream(streamingUrl(upstreamEndpoint), headers(inboundProtocol, resolved.account.apiKey), mapper.writeValueAsString(outboundRequest))
                .asFlow()
                .collect { raw ->
                    if (raw.contains("[DONE]")) sawDone = true
                    buffer.append(raw)
                    extractFrames(buffer, inboundProtocol, chunks)?.forEach { frame ->
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

            val usage = Usage(
                inputTokens = chunks.maxOfOrNull { it.usage?.inputTokens ?: 0L } ?: 0L,
                outputTokens = chunks.lastOrNull { it.usage?.outputTokens != null }?.usage?.outputTokens
                    ?: chunks.sumOf { it.usage?.outputTokens ?: 0L },
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
            usageRecorder.record(request.sessionId, resolved.account, resolved.provider, resolved.model, inboundProtocol, response, startedAt, "success")
            usageRecorder.updateSession(request.sessionId, response)
        } catch (cause: Throwable) {
            usageRecorder.record(
                sessionId = request.sessionId,
                account = resolved.account,
                providerName = resolved.provider,
                requestedModel = resolved.model,
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
                status = "failed"
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

    private fun resolveModel(qualifiedModel: String, protocol: Protocol): ResolvedModel {
        val separator = qualifiedModel.indexOf('/')
        require(separator > 0 && separator < qualifiedModel.length - 1) {
            "model must use <provider>/<model> format"
        }
        val provider = qualifiedModel.substring(0, separator)
        val model = qualifiedModel.substring(separator + 1)
        val catalog = modelRepository.findFirstByProviderAndModelIdAndEnabledTrue(provider, model)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown model $qualifiedModel")
        val account = accountRepository.findById(catalog.accountId).orElseThrow()
        require(account.enabled && account.supports(protocol)) {
            "Model $qualifiedModel is not available for $protocol"
        }
        return ResolvedModel(account, catalog.provider, catalog.modelId, qualifiedModel)
    }

    private fun upstreamUrl(endpoint: String): String = endpoint

    private fun streamingUrl(endpoint: String): String = endpoint

    private fun headers(protocol: Protocol, apiKey: String): Map<String, String> = when (protocol) {
        Protocol.OPENAI, Protocol.OPENAI_RESPONSES -> mapOf("Authorization" to "Bearer $apiKey")
        Protocol.ANTHROPIC -> mapOf("x-api-key" to apiKey, "anthropic-version" to "2023-06-01")
    }
}
