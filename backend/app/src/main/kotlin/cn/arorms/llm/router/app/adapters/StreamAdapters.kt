package cn.arorms.llm.router.app.adapters

import cn.arorms.llm.router.common.enums.Protocol
import cn.arorms.llm.router.common.enums.TokenInputSemantics
import cn.arorms.llm.router.common.responses.ChatStreamChunk
import cn.arorms.llm.router.common.responses.Usage
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant

object StreamAdapters {
    private val mapper: ObjectMapper = jacksonObjectMapper()

    fun parseUpstreamEvent(data: String?, protocol: Protocol): ChatStreamChunk? {
        if (data.isNullOrBlank() || data == "[DONE]") return null
        val body = runCatching { mapper.readTree(data) }.getOrNull() ?: return null
        return when (protocol) {
            Protocol.OPENAI -> parseOpenAi(body)
            Protocol.OPENAI_RESPONSES -> parseOpenAiResponses(body)
            Protocol.ANTHROPIC -> parseAnthropic(body)
        }
    }

    fun toInboundFrames(
        chunk: ChatStreamChunk?,
        protocol: Protocol,
        state: InboundStreamState
    ): List<SseFrame> {
        if (chunk == null) return emptyList()
        return when (protocol) {
            Protocol.OPENAI -> listOf(openAiFrame(chunk, state))
            Protocol.OPENAI_RESPONSES -> openAiResponsesFrames(chunk, state)
            Protocol.ANTHROPIC -> anthropicFrames(chunk, state)
        }
    }

    fun doneFrame(protocol: Protocol): SseFrame? = when (protocol) {
        Protocol.OPENAI -> SseFrame(data = "[DONE]")
        else -> null
    }

    private fun parseOpenAi(node: JsonNode): ChatStreamChunk {
        val choice = node.path("choices").get(0) ?: mapper.createObjectNode()
        val delta = choice.path("delta")
        val usage = node.path("usage").takeIf { it.isObject }
        return ChatStreamChunk(
            id = node.path("id").asText(""),
            model = node.path("model").asText(""),
            index = choice.path("index").asInt(0),
            delta = delta.path("content").asText(""),
            finishReason = choice.path("finish_reason").asText(null),
            usage = usage?.let {
                val inputTokens = it.path("prompt_tokens").asLong(0)
                val outputTokens = it.path("completion_tokens").asLong(0)
                val cacheReadTokens = usageLong(
                    it,
                    "/cache_read_input_tokens",
                    "/input_tokens_details/cached_tokens",
                    "/prompt_tokens_details/cached_tokens",
                    "/prompt_cache_hit_tokens"
                )
                val cacheCreationTokens = usageLong(
                    it,
                    "/cache_creation_input_tokens",
                    "/input_tokens_details/cache_write_tokens",
                    "/prompt_tokens_details/cache_write_tokens"
                )
                Usage(
                    inputTokens = inputTokens,
                    outputTokens = outputTokens,
                    totalTokens = it.path("total_tokens").asLong(inputTokens + outputTokens),
                    cacheReadTokens = cacheReadTokens,
                    cacheCreationTokens = cacheCreationTokens,
                    reasoningTokens = usageLong(
                        it,
                        "/completion_tokens_details/reasoning_tokens",
                        "/output_tokens_details/reasoning_tokens"
                    ).takeIf { value -> value > 0 },
                    inputTokenSemantics = TokenInputSemantics.TOTAL
                )
            }
        )
    }

    private fun parseOpenAiResponses(node: JsonNode): ChatStreamChunk? {
        val type = node.path("type").asText()
        return when (type) {
            "response.created" -> {
                val response = node.path("response")
                ChatStreamChunk(
                    id = response.path("id").asText(""),
                    model = response.path("model").asText(""),
                    index = 0,
                    delta = "",
                    finishReason = null,
                    usage = null
                )
            }
            "response.output_text.delta" -> ChatStreamChunk(
                id = "",
                model = "",
                index = node.path("output_index").asInt(0),
                delta = node.path("delta").asText(""),
                finishReason = null,
                usage = null
            )
            "response.completed" -> {
                val response = node.path("response")
                ChatStreamChunk(
                    id = response.path("id").asText(""),
                    model = response.path("model").asText(""),
                    index = 0,
                    delta = "",
                    finishReason = response.path("status").asText("completed"),
                    usage = response.path("usage").takeIf { it.isObject }?.let {
                        val inputTokens = it.path("input_tokens").asLong(0)
                        val outputTokens = it.path("output_tokens").asLong(0)
                        Usage(
                            inputTokens = inputTokens,
                            outputTokens = outputTokens,
                            totalTokens = it.path("total_tokens").asLong(inputTokens + outputTokens),
                            cacheReadTokens = usageLong(it, "/cache_read_input_tokens", "/input_tokens_details/cached_tokens"),
                            cacheCreationTokens = usageLong(it, "/cache_creation_input_tokens", "/input_tokens_details/cache_write_tokens"),
                            reasoningTokens = usageLong(it, "/output_tokens_details/reasoning_tokens").takeIf { value -> value > 0 },
                            inputTokenSemantics = TokenInputSemantics.TOTAL
                        )
                    }
                )
            }
            else -> null
        }
    }

    private fun parseAnthropic(node: JsonNode): ChatStreamChunk? {
        val type = node.path("type").asText()
        val message = node.path("message")
        return when (type) {
            "message_start" -> ChatStreamChunk(
                id = message.path("id").asText(""),
                model = message.path("model").asText(""),
                index = 0,
                delta = "",
                usage = message.path("usage").takeIf { it.isObject }?.let {
                    val inputTokens = it.path("input_tokens").asLong(0)
                    val cacheReadTokens = it.path("cache_read_input_tokens").asLong(0)
                    val cacheCreationTokens = it.path("cache_creation_input_tokens").asLong(0)
                    Usage(
                        inputTokens = inputTokens,
                        outputTokens = it.path("output_tokens").asLong(0),
                        totalTokens = inputTokens + it.path("output_tokens").asLong(0) + cacheReadTokens + cacheCreationTokens,
                        cacheReadTokens = cacheReadTokens,
                        cacheCreationTokens = cacheCreationTokens,
                        inputTokenSemantics = TokenInputSemantics.FRESH
                    )
                }
            )
            "content_block_delta" -> ChatStreamChunk(
                id = "",
                model = "",
                index = node.path("index").asInt(0),
                delta = node.path("delta").path("text").asText(""),
                finishReason = null,
                usage = null
            )
            "message_delta" -> ChatStreamChunk(
                id = "",
                model = "",
                index = 0,
                delta = "",
                finishReason = node.path("delta").path("stop_reason").asText(null),
                usage = node.path("usage").takeIf { it.isObject }?.let {
                    val outputTokens = it.path("output_tokens").asLong(0)
                    val cacheReadTokens = it.path("cache_read_input_tokens").asLong(0)
                    val cacheCreationTokens = it.path("cache_creation_input_tokens").asLong(0)
                    Usage(
                        inputTokens = it.path("input_tokens").asLong(0),
                        outputTokens = outputTokens,
                        totalTokens = outputTokens + cacheReadTokens + cacheCreationTokens,
                        cacheReadTokens = cacheReadTokens,
                        cacheCreationTokens = cacheCreationTokens,
                        inputTokenSemantics = TokenInputSemantics.FRESH
                    )
                }
            )
            else -> null
        }
    }

    private fun parseGemini(node: JsonNode): ChatStreamChunk {
        val candidate = node.path("candidates").get(0) ?: mapper.createObjectNode()
        val text = candidate.path("content").path("parts")
            .joinToString("") { it.path("text").asText("") }
        val usage = node.path("usageMetadata").takeIf { it.isObject }
        return ChatStreamChunk(
            id = node.path("responseId").asText(""),
            model = node.path("modelVersion").asText(""),
            index = 0,
            delta = text,
            finishReason = candidate.path("finishReason").asText(null),
            usage = usage?.let {
                Usage(
                    inputTokens = it.path("promptTokenCount").asLong(0),
                    outputTokens = it.path("candidatesTokenCount").asLong(0)
                )
            }
        )
    }

    private fun usageLong(node: JsonNode, vararg paths: String): Long =
        paths.firstNotNullOfOrNull { node.at(it).takeIf { value -> value.isNumber }?.asLong() } ?: 0L

    private fun openAiFrame(chunk: ChatStreamChunk?, state: InboundStreamState): SseFrame {
        if (chunk == null) return SseFrame(data = "[DONE]")
        val body = mapper.createObjectNode()
        body.put("id", state.id.ifBlank { chunk.id.ifBlank { state.fallbackId() } })
        body.put("object", "chat.completion.chunk")
        body.put("created", Instant.now().epochSecond)
        body.put("model", state.model.ifBlank { chunk.model })
        val choice = body.putArray("choices").addObject()
        choice.put("index", chunk.index)
        val delta = choice.putObject("delta")
        if (chunk.delta.isNotEmpty()) delta.put("content", chunk.delta)
        chunk.finishReason?.let { choice.put("finish_reason", it) }
        chunk.usage?.let { usage ->
            val usageNode = body.putObject("usage")
            usageNode.put("prompt_tokens", usage.inputTokens)
            usageNode.put("completion_tokens", usage.outputTokens)
            usageNode.put("total_tokens", usage.totalTokens)
        }
        return SseFrame(data = mapper.writeValueAsString(body))
    }

    private fun openAiResponsesFrames(chunk: ChatStreamChunk, state: InboundStreamState): List<SseFrame> {
        val frames = mutableListOf<SseFrame>()
        if (chunk.id.isNotBlank() && !state.messageStarted) {
            state.messageStarted = true
            state.id = chunk.id
            state.model = chunk.model.ifBlank { state.model }
            val created = mapper.createObjectNode()
                .put("type", "response.created")
                .putObject("response")
                .put("id", state.id)
                .put("model", state.model)
            frames += SseFrame("response.created", mapper.writeValueAsString(created))
        }
        if (chunk.delta.isNotEmpty()) {
            val delta = mapper.createObjectNode()
                .put("type", "response.output_text.delta")
                .put("delta", chunk.delta)
            frames += SseFrame("response.output_text.delta", mapper.writeValueAsString(delta))
        }
        if (chunk.finishReason != null) {
            val completed = mapper.createObjectNode()
                .put("type", "response.completed")
                .putObject("response")
                .put("id", state.id)
                .put("model", state.model)
                .put("status", chunk.finishReason)
            chunk.usage?.let { usage ->
                completed.putObject("usage")
                    .put("input_tokens", usage.inputTokens)
                    .put("output_tokens", usage.outputTokens)
                    .put("total_tokens", usage.totalTokens)
            }
            frames += SseFrame("response.completed", mapper.writeValueAsString(completed))
        }
        return frames
    }

    private fun anthropicFrames(chunk: ChatStreamChunk?, state: InboundStreamState): List<SseFrame> {
        if (chunk == null) return emptyList()
        val frames = mutableListOf<SseFrame>()

        if (!state.messageStarted) {
            state.messageStarted = true
            if (chunk.id.isNotBlank()) state.id = chunk.id
            if (chunk.model.isNotBlank()) state.model = chunk.model
            chunk.usage?.inputTokens?.takeIf { it > 0 }?.let { state.inputTokens = it }
            val messageStart = mapper.createObjectNode()
                .put("type", "message_start")
                .putObject("message")
                .apply {
                    put("id", state.id)
                    put("type", "message")
                    put("role", "assistant")
                    put("model", state.model)
                    putObject("usage")
                        .put("input_tokens", state.inputTokens)
                        .put("output_tokens", 0)
                }
            frames += SseFrame("message_start", mapper.writeValueAsString(messageStart))
        }

        if (chunk.delta.isNotEmpty()) {
            if (!state.blockStarted) {
                state.blockStarted = true
                val blockStart = mapper.createObjectNode()
                    .put("type", "content_block_start")
                    .put("index", 0)
                    .putObject("content_block")
                    .put("type", "text_delta")
                    .put("text", "")
                frames += SseFrame("content_block_start", mapper.writeValueAsString(blockStart))
            }
            val blockDelta = mapper.createObjectNode()
                .put("type", "content_block_delta")
                .put("index", 0)
                .putObject("delta")
                .put("type", "text_delta")
                .put("text", chunk.delta)
            frames += SseFrame("content_block_delta", mapper.writeValueAsString(blockDelta))
        }

        chunk.usage?.outputTokens?.takeIf { it > 0 }?.let { state.outputTokens = it }
        if (chunk.finishReason != null) {
            if (state.blockStarted) {
                state.blockStarted = false
                frames += SseFrame("content_block_stop", mapper.writeValueAsString(mapper.createObjectNode().put("type", "content_block_stop").put("index", 0)))
            }
            val messageDelta = mapper.createObjectNode()
                .put("type", "message_delta")
                .putObject("delta")
                .put("stop_reason", chunk.finishReason)
                .put("stop_sequence", "")
            messageDelta.putObject("usage").put("output_tokens", state.outputTokens)
            frames += SseFrame("message_delta", mapper.writeValueAsString(messageDelta))
            frames += SseFrame("message_stop", mapper.writeValueAsString(mapper.createObjectNode().put("type", "message_stop")))
        }
        return frames
    }

    private fun geminiFrame(chunk: ChatStreamChunk?, state: InboundStreamState): SseFrame {
        if (chunk == null) return SseFrame(data = "{}")
        val body = mapper.createObjectNode()
        val candidate = body.putArray("candidates").addObject()
        val content = candidate.putObject("content")
        content.put("role", "model")
        val parts = content.putArray("parts")
        if (chunk.delta.isNotEmpty()) parts.addObject().put("text", chunk.delta)
        chunk.finishReason?.let { candidate.put("finishReason", it) }
        chunk.usage?.let { usage ->
            val usageNode = body.putObject("usageMetadata")
            usageNode.put("promptTokenCount", usage.inputTokens)
            usageNode.put("candidatesTokenCount", usage.outputTokens)
            usageNode.put("totalTokenCount", usage.totalTokens)
        }
        return SseFrame(data = mapper.writeValueAsString(body))
    }
}

class InboundStreamState(
    val requestedModel: String,
    val requestedProtocol: cn.arorms.llm.router.common.enums.Protocol,
    initialId: String = ""
) {
    var id: String = initialId
    var model: String = requestedModel
    var messageStarted: Boolean = false
    var blockStarted: Boolean = false
    var inputTokens: Long = 0
    var outputTokens: Long = 0

    fun fallbackId(): String = "chatcmpl-${Instant.now().toEpochMilli()}"
}
