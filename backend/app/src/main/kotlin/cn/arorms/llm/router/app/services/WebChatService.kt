package cn.arorms.llm.router.app.services

import cn.arorms.llm.router.app.entities.ChatMessage
import cn.arorms.llm.router.app.repositories.ChatMessageRepository
import cn.arorms.llm.router.common.enums.ChatMessageStatus
import cn.arorms.llm.router.common.enums.Protocol
import cn.arorms.llm.router.common.requests.WebChatRequest
import cn.arorms.llm.router.common.responses.WebChatMessageResponse
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WebChatService(
    private val repository: ChatMessageRepository,
    private val mapper: ObjectMapper
) {
    @Transactional
    fun history(): List<WebChatMessageResponse> =
        repository.findByConversationKeyOrderByIdAsc(DEFAULT_CONVERSATION_KEY).map { it.toResponse() }

    @Transactional
    fun clear() {
        repository.deleteByConversationKey(DEFAULT_CONVERSATION_KEY)
    }

    @Transactional
    fun appendUser(request: WebChatRequest): List<ChatMessage> {
        repository.save(
            ChatMessage(
                conversationKey = DEFAULT_CONVERSATION_KEY,
                role = "user",
                content = request.content.trim(),
                model = request.model,
                protocol = request.protocol,
                status = ChatMessageStatus.COMPLETED,
                metadata = buildMap {
                    request.imageDataUrl?.let { put("imageDataUrl", it) }
                }
            )
        )
        return repository.findByConversationKeyOrderByIdAsc(DEFAULT_CONVERSATION_KEY)
    }

    @Transactional
    fun appendAssistant(content: String, protocol: Protocol, model: String) {
        repository.save(
            ChatMessage(
                conversationKey = DEFAULT_CONVERSATION_KEY,
                role = "assistant",
                content = content,
                model = model,
                protocol = protocol,
                status = ChatMessageStatus.COMPLETED
            )
        )
    }

    @Transactional
    fun appendFailed(protocol: Protocol, model: String, error: String, partialContent: String) {
        repository.save(
            ChatMessage(
                conversationKey = DEFAULT_CONVERSATION_KEY,
                role = "assistant",
                content = partialContent,
                model = model,
                protocol = protocol,
                status = ChatMessageStatus.FAILED,
                metadata = mapOf("error" to error)
            )
        )
    }

    fun buildGatewayBody(request: WebChatRequest, history: List<ChatMessage>): String {
        val body = mapper.createObjectNode()
        body.put("model", request.model)
        body.put("stream", true)

        when (request.protocol) {
            Protocol.OPENAI -> buildOpenAiBody(body, request, history)
            Protocol.OPENAI_RESPONSES -> buildResponsesBody(body, request, history)
            Protocol.ANTHROPIC -> buildAnthropicBody(body, request, history)
        }
        return mapper.writeValueAsString(body)
    }

    fun extractDelta(protocol: Protocol, data: String?): String {
        if (data.isNullOrBlank() || data == "[DONE]") return ""
        val body = runCatching { mapper.readTree(data) }.getOrNull() ?: return ""
        return when (protocol) {
            Protocol.OPENAI -> body.path("choices").get(0)?.path("delta")?.path("content")?.asText("") ?: ""
            Protocol.OPENAI_RESPONSES -> if (body.path("type").asText() == "response.output_text.delta") body.path("delta").asText("") else ""
            Protocol.ANTHROPIC -> if (body.path("type").asText() == "content_block_delta") body.path("delta").path("text").asText("") else ""
        }
    }

    private fun buildOpenAiBody(body: com.fasterxml.jackson.databind.node.ObjectNode, request: WebChatRequest, history: List<ChatMessage>) {
        val messages = body.putArray("messages")
        history.forEach { message ->
            val item = messages.addObject()
            item.put("role", message.role)
            val image = message.metadata?.get("imageDataUrl") as? String
            if (message.role == "user" && !image.isNullOrBlank()) {
                val content = item.putArray("content")
                content.addObject().put("type", "text").put("text", message.content)
                content.addObject().put("type", "image_url").putObject("image_url").put("url", image)
            } else {
                item.put("content", message.content)
            }
        }
        body.put("max_tokens", 4096)
        request.temperature?.let { body.put("temperature", it) }
        if (request.thinkingEffort.name != "NONE") body.put("reasoning_effort", request.thinkingEffort.name.lowercase())
        if (request.tools.isNotEmpty()) body.set<JsonNode>("tools", mapper.valueToTree(request.tools))
    }

    private fun buildResponsesBody(body: com.fasterxml.jackson.databind.node.ObjectNode, request: WebChatRequest, history: List<ChatMessage>) {
        val input = body.putArray("input")
        history.forEach { message ->
            val item = input.addObject()
            item.put("type", "message")
            item.put("role", message.role)
            val image = message.metadata?.get("imageDataUrl") as? String
            val content = item.putArray("content")
            content.addObject().put("type", if (message.role == "assistant") "output_text" else "input_text").put("text", message.content)
            if (!image.isNullOrBlank()) content.addObject().put("type", "input_image").put("image_url", image)
        }
        body.put("max_output_tokens", 4096)
        request.temperature?.let { body.put("temperature", it) }
        if (request.thinkingEffort.name != "NONE") body.putObject("reasoning").put("effort", request.thinkingEffort.name.lowercase())
        if (request.tools.isNotEmpty()) body.set<JsonNode>("tools", mapper.valueToTree(request.tools))
    }

    private fun buildAnthropicBody(body: com.fasterxml.jackson.databind.node.ObjectNode, request: WebChatRequest, history: List<ChatMessage>) {
        body.put("max_tokens", 4096)
        request.temperature?.let { body.put("temperature", it) }
        val messages = body.putArray("messages")
        history.forEach { message ->
            val item = messages.addObject()
            item.put("role", message.role)
            val image = message.metadata?.get("imageDataUrl") as? String
            if (message.role == "user" && !image.isNullOrBlank()) {
                val blocks = item.putArray("content")
                blocks.addObject().put("type", "text").put("text", message.content)
                blocks.addObject().put("type", "image").putObject("source")
                    .put("type", "base64")
                    .put("media_type", image.substringAfter("data:").substringBefore(";base64,").ifBlank { "image/png" })
                    .put("data", image.substringAfter(";base64,", ""))
            } else {
                item.put("content", message.content)
            }
        }
        if (request.tools.isNotEmpty()) body.set<JsonNode>("tools", mapper.valueToTree(request.tools))
    }

    private fun ChatMessage.toResponse() = WebChatMessageResponse(
        id = id ?: 0L,
        role = role,
        content = content,
        model = model,
        protocol = protocol,
        status = status,
        createdAt = createdAt?.toString()
    )

    companion object {
        const val DEFAULT_CONVERSATION_KEY = "default"
    }
}
