package cn.arorms.llm.router.app.adapters

import cn.arorms.llm.router.common.enums.PartType
import cn.arorms.llm.router.common.enums.Protocol
import cn.arorms.llm.router.common.enums.ThinkingEffort
import cn.arorms.llm.router.common.requests.ChatRequest
import cn.arorms.llm.router.common.responses.ChatChoice
import cn.arorms.llm.router.common.responses.ChatMessage
import cn.arorms.llm.router.common.responses.ChatPart
import cn.arorms.llm.router.common.responses.ChatResponse
import cn.arorms.llm.router.common.responses.ToolDefinition
import cn.arorms.llm.router.common.responses.Usage
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant
import java.util.UUID

object Adapters {
    private val mapper = jacksonObjectMapper()

    fun parseRequest(body: JsonNode, protocol: Protocol): ChatRequest = when (protocol) {
        Protocol.OPENAI -> parseOpenAiRequest(body)
        Protocol.OPENAI_RESPONSES -> parseOpenAiResponsesRequest(body)
        Protocol.ANTHROPIC -> parseAnthropicRequest(body)
        // Protocol.GEMINI -> parseGeminiRequest(body)
    }

    fun toOutboundRequest(request: ChatRequest, protocol: Protocol): JsonNode = when (protocol) {
        Protocol.OPENAI -> openAiRequest(request)
        Protocol.OPENAI_RESPONSES -> openAiResponsesRequest(request)
        Protocol.ANTHROPIC -> anthropicRequest(request)
        // Protocol.GEMINI -> geminiRequest(request)
    }

    fun parseResponse(body: JsonNode, request: ChatRequest, provider: String, accountName: String?): ChatResponse {
        val usage = when (provider.uppercase()) {
            "ANTHROPIC" -> parseAnthropicUsage(body)
            "OPENAI_RESPONSES" -> parseOpenAiResponsesUsage(body)
            else -> parseOpenAiUsage(body)
        }
        return ChatResponse(
            id = body.path("id").asText(body.path("responseId").asText(UUID.randomUUID().toString())),
            model = body.path("model").asText(body.path("modelVersion").asText(request.model)),
            choices = when (provider.uppercase()) {
                "ANTHROPIC" -> listOf(parseAnthropicChoice(body))
                "OPENAI_RESPONSES" -> listOf(parseOpenAiResponsesChoice(body))
                else -> parseOpenAiChoices(body)
            },
            usage = usage,
            provider = provider,
            accountName = accountName,
            createdAt = Instant.now().epochSecond
        )
    }

    fun toInboundResponse(response: ChatResponse, protocol: Protocol): JsonNode = when (protocol) {
        Protocol.OPENAI -> openAiResponse(response)
        Protocol.OPENAI_RESPONSES -> openAiResponsesResponse(response)
        Protocol.ANTHROPIC -> anthropicResponse(response)
        // Protocol.GEMINI -> geminiResponse(response)
    }

    private fun parseOpenAiRequest(body: JsonNode): ChatRequest = ChatRequest(
        model = body.path("model").asText(),
        messages = body.path("messages").map { message ->
            val role = message.path("role").asText("user")
            val parts = when (val content = message.path("content")) {
                is ArrayNode -> content.map { part ->
                    when (part.path("type").asText()) {
                        "image_url" -> {
                        val url = part.path("image_url").path("url").asText()
                        if (url.startsWith("data:")) {
                            val mediaType = url.substringAfter("data:").substringBefore(";base64,")
                            val data = url.substringAfter(";base64,", "")
                            ChatPart(PartType.IMAGE_DATA, mimeType = mediaType.ifBlank { "image/png" }, data = data)
                        } else ChatPart(PartType.IMAGE_URL, imageUrl = url)
                    }
                        "tool_result" -> ChatPart(
                            PartType.TOOL_RESULT,
                            text = part.path("content").asText(),
                            toolCallId = part.path("tool_call_id").asText(),
                            name = part.path("name").asText(null)
                        )
                        else -> ChatPart(PartType.TEXT, text = part.path("text").asText())
                    }
                }
                else -> listOf(ChatPart(PartType.TEXT, text = content.asText("")))
            }
            ChatMessage(role, parts)
        },
        maxTokens = body.path("max_tokens").asInt(body.path("max_completion_tokens").asInt(0)).takeIf { it > 0 },
        temperature = body.path("temperature").asDouble(1.0).takeIf { body.has("temperature") },
        topP = body.path("top_p").asDouble(1.0).takeIf { body.has("top_p") },
        tools = parseOpenAiTools(body.path("tools")),
        thinkingEffort = body.path("reasoning_effort").asText("none").uppercase().let {
            runCatching { ThinkingEffort.valueOf(it) }.getOrDefault(ThinkingEffort.NONE)
        },
        sessionId = body.path("session_id").asText(body.path("metadata").path("session_id").asText(null)),
        stream = body.path("stream").asBoolean(false),
        extra = body.path("metadata").let { if (it.isObject) mapper.convertValue(it, Map::class.java) as Map<String, Any?> else emptyMap() }
    )

    private fun parseOpenAiResponsesRequest(body: JsonNode): ChatRequest {
        val messages = mutableListOf<ChatMessage>()
        body.path("instructions").asText(null)?.takeIf { it.isNotBlank() }?.let {
            messages += ChatMessage("system", listOf(ChatPart(PartType.TEXT, text = it)))
        }
        when (val input = body.path("input")) {
            is ArrayNode -> input.forEach { item ->
                val role = item.path("role").asText("user")
                val parts = when (val content = item.path("content")) {
                    is ArrayNode -> content.map { part ->
                        when (part.path("type").asText()) {
                            "output_text", "text" -> ChatPart(PartType.TEXT, text = part.path("text").asText())
                            "input_image" -> ChatPart(PartType.IMAGE_URL, imageUrl = part.path("image_url").asText(null))
                            else -> ChatPart(PartType.TEXT, text = part.path("text").asText())
                        }
                    }
                    else -> listOf(ChatPart(PartType.TEXT, text = content.asText("")))
                }
                messages += ChatMessage(role, parts)
            }
            else -> messages += ChatMessage("user", listOf(ChatPart(PartType.TEXT, text = input.asText(""))))
        }
        return ChatRequest(
            model = body.path("model").asText(),
            messages = messages,
            maxTokens = body.path("max_output_tokens").asInt(0).takeIf { it > 0 },
            temperature = body.path("temperature").asDouble(1.0).takeIf { body.has("temperature") },
            topP = body.path("top_p").asDouble(1.0).takeIf { body.has("top_p") },
            tools = body.path("tools").map { tool ->
                ToolDefinition(
                    name = tool.path("name").asText(),
                    description = tool.path("description").asText(null),
                    schema = if (tool.path("parameters").isObject) mapper.convertValue(tool.path("parameters"), Map::class.java) as Map<String, Any?> else null
                )
            },
            thinkingEffort = body.path("reasoning").path("effort").asText("none").uppercase().let {
                runCatching { ThinkingEffort.valueOf(it) }.getOrDefault(ThinkingEffort.NONE)
            },
            sessionId = body.path("metadata").path("session_id").asText(null),
            stream = body.path("stream").asBoolean(false)
        )
    }

    private fun parseAnthropicRequest(body: JsonNode): ChatRequest {
        val messages = mutableListOf<ChatMessage>()
        val system = body.path("system")
        when {
            system.isTextual && system.asText().isNotBlank() -> messages += ChatMessage("system", listOf(ChatPart(PartType.TEXT, system.asText())))
            system is ArrayNode && system.size() > 0 -> messages += ChatMessage("system", system.map { ChatPart(PartType.TEXT, it.path("text").asText()) })
        }
        body.path("messages").forEach { message ->
            val parts = when (val content = message.path("content")) {
                is ArrayNode -> content.map { part ->
                    when (part.path("type").asText()) {
                        "image" -> ChatPart(
                            PartType.IMAGE_DATA,
                            mimeType = part.path("source").path("media_type").asText(),
                            data = part.path("source").path("data").asText()
                        )
                        "tool_result" -> ChatPart(
                            PartType.TOOL_RESULT,
                            text = part.path("content").asText(),
                            toolCallId = part.path("tool_use_id").asText()
                        )
                        else -> ChatPart(PartType.TEXT, text = part.path("text").asText())
                    }
                }
                else -> listOf(ChatPart(PartType.TEXT, content.asText("")))
            }
            messages += ChatMessage(message.path("role").asText("user"), parts)
        }
        return ChatRequest(
            model = body.path("model").asText(),
            messages = messages,
            maxTokens = body.path("max_tokens").asInt(4096),
            temperature = body.path("temperature").asDouble(1.0).takeIf { body.has("temperature") },
            topP = body.path("top_p").asDouble(1.0).takeIf { body.has("top_p") },
            tools = body.path("tools").map {
                ToolDefinition(
                    name = it.path("name").asText(),
                    description = it.path("description").asText(null),
                    schema = if (it.path("input_schema").isObject) mapper.convertValue(it.path("input_schema"), Map::class.java) as Map<String, Any?> else null
                )
            },
            thinkingEffort = if (body.path("thinking").path("type").asText() == "enabled") ThinkingEffort.HIGH else ThinkingEffort.NONE,
            sessionId = body.path("metadata").path("session_id").asText(null),
            stream = body.path("stream").asBoolean(false)
        )
    }

    private fun parseGeminiRequest(body: JsonNode): ChatRequest {
        val messages = mutableListOf<ChatMessage>()
        body.path("systemInstruction").path("parts").forEach { messages += ChatMessage("system", listOf(ChatPart(PartType.TEXT, it.path("text").asText()))) }
        body.path("contents").forEach { content ->
            messages += ChatMessage(
                role = when (content.path("role").asText("user")) { "model" -> "assistant"; else -> content.path("role").asText() },
                parts = content.path("parts").map { part ->
                    if (part.has("inlineData")) ChatPart(PartType.IMAGE_DATA, mimeType = part.path("inlineData").path("mimeType").asText(), data = part.path("inlineData").path("data").asText())
                    else ChatPart(PartType.TEXT, text = part.path("text").asText())
                }
            )
        }
        val config = body.path("generationConfig")
        return ChatRequest(
            model = body.path("model").asText(),
            messages = messages,
            maxTokens = config.path("maxOutputTokens").asInt(0).takeIf { it > 0 },
            temperature = config.path("temperature").asDouble(1.0).takeIf { config.has("temperature") },
            topP = config.path("topP").asDouble(1.0).takeIf { config.has("topP") },
            tools = body.path("tools").flatMap { it.path("functionDeclarations").map { declaration ->
                ToolDefinition(
                    name = declaration.path("name").asText(),
                    description = declaration.path("description").asText(null),
                    schema = if (declaration.path("parameters").isObject) mapper.convertValue(declaration.path("parameters"), Map::class.java) as Map<String, Any?> else null
                )
            } },
            thinkingEffort = config.path("thinkingConfig").path("thinkingBudget").asInt(0).let { budget ->
                when { budget > 2048 -> ThinkingEffort.HIGH; budget > 512 -> ThinkingEffort.MEDIUM; budget > 0 -> ThinkingEffort.LOW; else -> ThinkingEffort.NONE }
            },
            stream = false
        )
    }

    private fun parseOpenAiTools(node: JsonNode): List<ToolDefinition> = node.mapNotNull {
        val function = it.path("function")
        if (it.path("type").asText() != "function" || function.path("name").asText().isBlank()) null
        else ToolDefinition(
            name = function.path("name").asText(),
            description = function.path("description").asText(null),
            schema = if (function.path("parameters").isObject) mapper.convertValue(function.path("parameters"), Map::class.java) as Map<String, Any?> else null
        )
    }

    private fun openAiRequest(request: ChatRequest): JsonNode {
        val body = mapper.createObjectNode()
        body.put("model", request.model)
        val messages = body.putArray("messages")
        request.messages.forEach { message ->
            val item = messages.addObject()
            item.put("role", message.role)
            val parts = item.putArray("content")
            message.parts.forEach { part ->
                when (part.type) {
                    PartType.TEXT -> parts.addObject().put("type", "text").put("text", part.text ?: "")
                    PartType.IMAGE_URL -> parts.addObject().put("type", "image_url").set("image_url", mapper.createObjectNode().put("url", part.imageUrl))
                    PartType.IMAGE_DATA -> parts.addObject().put("type", "image_url").set("image_url", mapper.createObjectNode().put("url", "data:${part.mimeType};base64,${part.data}"))
                    PartType.TOOL_RESULT -> {
                        item.put("role", "tool")
                        item.put("tool_call_id", part.toolCallId ?: "")
                        item.put("content", part.text ?: "")
                    }
                }
            }
            if (message.parts.isEmpty()) item.put("content", "")
        }
        request.maxTokens?.let { body.put("max_tokens", it) }
        request.temperature?.let { body.put("temperature", it) }
        request.topP?.let { body.put("top_p", it) }
        if (request.thinkingEffort != ThinkingEffort.NONE) body.put("reasoning_effort", request.thinkingEffort.name.lowercase())
        if (request.tools.isNotEmpty()) {
            val tools = body.putArray("tools")
            request.tools.forEach { tool ->
                val item = tools.addObject().put("type", "function")
                val function = item.putObject("function")
                function.put("name", tool.name)
                tool.description?.let { function.put("description", it) }
                function.set<JsonNode>("parameters", if (tool.schema == null) mapper.createObjectNode().put("type", "object") as JsonNode else mapper.valueToTree(tool.schema))
            }
        }
        body.put("stream", request.stream)
        return body
    }

    private fun openAiResponsesRequest(request: ChatRequest): JsonNode {
        val body = mapper.createObjectNode()
        body.put("model", request.model)
        val instructions = request.messages.filter { it.role == "system" }
            .flatMap { it.parts }
            .joinToString("\n") { it.text ?: "" }
        if (instructions.isNotBlank()) body.put("instructions", instructions)
        val input = body.putArray("input")
        request.messages.filter { it.role != "system" }.forEach { message ->
            val item = input.addObject()
            item.put("type", "message")
            item.put("role", message.role)
            val content = item.putArray("content")
            message.parts.forEach { part ->
                when (part.type) {
                    PartType.TEXT -> content.addObject().put("type", if (message.role == "assistant") "output_text" else "input_text").put("text", part.text ?: "")
                    PartType.IMAGE_URL -> content.addObject().put("type", "input_image").put("image_url", part.imageUrl ?: "")
                    PartType.IMAGE_DATA -> content.addObject().put("type", "input_image").put("image_url", "data:${part.mimeType};base64,${part.data}")
                    PartType.TOOL_RESULT -> content.addObject().put("type", "input_text").put("text", part.text ?: "")
                }
            }
        }
        request.maxTokens?.let { body.put("max_output_tokens", it) }
        request.temperature?.let { body.put("temperature", it) }
        request.topP?.let { body.put("top_p", it) }
        if (request.thinkingEffort != ThinkingEffort.NONE) {
            val effort = when (request.thinkingEffort) {
                ThinkingEffort.MINIMAL -> "minimal"
                ThinkingEffort.LOW -> "low"
                ThinkingEffort.MEDIUM -> "medium"
                else -> "high"
            }
            body.putObject("reasoning").put("effort", effort)
        }
        if (request.tools.isNotEmpty()) {
            val tools = body.putArray("tools")
            request.tools.forEach { tool ->
                val item = tools.addObject().put("type", "function")
                item.put("name", tool.name)
                tool.description?.let { item.put("description", it) }
                val parameters: JsonNode = if (tool.schema == null) mapper.createObjectNode().put("type", "object") else mapper.valueToTree(tool.schema)
                item.set<JsonNode>("parameters", parameters)
            }
        }
        body.put("stream", request.stream)
        return body
    }

    private fun anthropicRequest(request: ChatRequest): JsonNode {
        val body = mapper.createObjectNode()
        body.put("model", request.model)
        body.put("max_tokens", request.maxTokens ?: 4096)
        val systemMessages = request.messages.filter { it.role == "system" }
        if (systemMessages.isNotEmpty()) {
            val system = body.putArray("system")
            systemMessages.forEach { message -> message.parts.forEach { system.addObject().put("type", "text").put("text", it.text ?: "") } }
        }
        val messages = body.putArray("messages")
        request.messages.filter { it.role != "system" }.forEach { message ->
            val item = messages.addObject().put("role", message.role)
            val blocks = item.putArray("content")
            message.parts.forEach { part ->
                when (part.type) {
                    PartType.TEXT -> blocks.addObject().put("type", "text").put("text", part.text ?: "")
                    PartType.IMAGE_DATA -> {
                        val source = blocks.addObject().put("type", "image").putObject("source")
                        source.put("type", "base64"); source.put("media_type", part.mimeType ?: "image/png"); source.put("data", part.data)
                    }
                    PartType.IMAGE_URL -> blocks.addObject().put("type", "text").put("text", part.imageUrl ?: "")
                    PartType.TOOL_RESULT -> {
                        val block = blocks.addObject().put("type", "tool_result")
                        block.put("tool_use_id", part.toolCallId)
                        block.put("content", part.text ?: "")
                    }
                }
            }
        }
        request.temperature?.let { body.put("temperature", it) }
        request.topP?.let { body.put("top_p", it) }
        if (request.thinkingEffort == ThinkingEffort.HIGH) body.set<JsonNode>("thinking", mapper.createObjectNode().put("type", "enabled").put("budget_tokens", request.maxTokens ?: 4096))
        if (request.tools.isNotEmpty()) {
            val tools = body.putArray("tools")
            request.tools.forEach { tool ->
                val item = tools.addObject()
                item.put("name", tool.name)
                tool.description?.let { item.put("description", it) }
                item.set<JsonNode>("input_schema", if (tool.schema == null) mapper.createObjectNode().put("type", "object") as JsonNode else mapper.valueToTree(tool.schema))
            }
        }
        body.put("stream", request.stream)
        return body
    }

    private fun geminiRequest(request: ChatRequest): JsonNode {
        val body = mapper.createObjectNode()
        val systemParts = request.messages.filter { it.role == "system" }.flatMap { it.parts }
        if (systemParts.isNotEmpty()) {
            val instruction = body.putObject("systemInstruction").putArray("parts")
            systemParts.forEach { instruction.addObject().put("text", it.text ?: "") }
        }
        val contents = body.putArray("contents")
        request.messages.filter { it.role != "system" }.forEach { message ->
            val content = contents.addObject()
            content.put("role", if (message.role == "assistant") "model" else message.role)
            val parts = content.putArray("parts")
            message.parts.forEach { part ->
                when (part.type) {
                    PartType.TEXT -> parts.addObject().put("text", part.text ?: "")
                    PartType.IMAGE_DATA -> {
                        val inline = parts.addObject().putObject("inlineData")
                        inline.put("mimeType", part.mimeType ?: "image/png"); inline.put("data", part.data)
                    }
                    PartType.IMAGE_URL -> parts.addObject().put("text", part.imageUrl ?: "")
                    PartType.TOOL_RESULT -> parts.addObject().put("text", part.text ?: "")
                }
            }
        }
        val config = body.putObject("generationConfig")
        request.maxTokens?.let { config.put("maxOutputTokens", it) }
        request.temperature?.let { config.put("temperature", it) }
        request.topP?.let { config.put("topP", it) }
        if (request.thinkingEffort != ThinkingEffort.NONE) {
            val budget = when (request.thinkingEffort) { ThinkingEffort.HIGH -> 4096; ThinkingEffort.MEDIUM -> 1024; ThinkingEffort.LOW -> 256; else -> 0 }
            config.putObject("thinkingConfig").put("thinkingBudget", budget)
        }
        if (request.tools.isNotEmpty()) {
            val declarations = body.putArray("tools").addObject().putArray("functionDeclarations")
            request.tools.forEach { tool ->
                val declaration = declarations.addObject()
                declaration.put("name", tool.name)
                tool.description?.let { declaration.put("description", it) }
                declaration.set<JsonNode>("parameters", if (tool.schema == null) mapper.createObjectNode().put("type", "object") as JsonNode else mapper.valueToTree(tool.schema))
            }
        }
        return body
    }

    private fun parseOpenAiChoices(body: JsonNode): List<ChatChoice> = body.path("choices").map { choice ->
        val message = choice.path("message")
        val parts = mutableListOf<ChatPart>()
        when (val content = message.path("content")) {
            is ArrayNode -> content.forEach { parts += ChatPart(PartType.TEXT, text = it.path("text").asText()) }
            else -> parts += ChatPart(PartType.TEXT, text = content.asText(""))
        }
        val toolParts = message.path("tool_calls").map { call ->
            ChatPart(PartType.TEXT, text = call.path("function").path("arguments").asText(), toolCallId = call.path("id").asText(), name = call.path("function").path("name").asText())
        }
        val messageWithToolCalls = parts + toolParts
        ChatChoice(choice.path("index").asInt(0), ChatMessage(message.path("role").asText("assistant"), messageWithToolCalls), choice.path("finish_reason").asText(null))
    }

    private fun parseAnthropicChoice(body: JsonNode): ChatChoice {
        val parts = body.path("content").map { block ->
            when (block.path("type").asText()) {
                "tool_use" -> ChatPart(PartType.TEXT, text = mapper.writeValueAsString(block.path("input")), toolCallId = block.path("id").asText(), name = block.path("name").asText())
                else -> ChatPart(PartType.TEXT, text = block.path("text").asText())
            }
        }
        return ChatChoice(0, ChatMessage("assistant", parts), body.path("stop_reason").asText())
    }

    private fun parseGeminiChoices(body: JsonNode): List<ChatChoice> = body.path("candidates").map { candidate ->
        val parts = candidate.path("content").path("parts").map { part ->
            if (part.has("functionCall")) ChatPart(PartType.TEXT, text = mapper.writeValueAsString(part.path("functionCall").path("args")), toolCallId = part.path("functionCall").path("name").asText(), name = part.path("functionCall").path("name").asText())
            else ChatPart(PartType.TEXT, text = part.path("text").asText())
        }
        ChatChoice(0, ChatMessage("assistant", parts), candidate.path("finishReason").asText(null))
    }

    private fun parseOpenAiUsage(body: JsonNode): Usage? {
        val usage = body.path("usage")
        if (!usage.isObject) return null
        return Usage(usage.path("prompt_tokens").asLong(0), usage.path("completion_tokens").asLong(0))
    }

    private fun parseOpenAiResponsesChoice(body: JsonNode): ChatChoice {
        val parts = body.path("output").filter { it.path("type").asText() == "message" }
            .flatMap { item -> item.path("content").map { part ->
                when (part.path("type").asText()) {
                    "output_text" -> ChatPart(PartType.TEXT, text = part.path("text").asText())
                    "function_call" -> ChatPart(
                        PartType.TEXT,
                        text = part.path("arguments").asText("{}"),
                        toolCallId = part.path("call_id").asText(null),
                        name = part.path("name").asText(null)
                    )
                    else -> ChatPart(PartType.TEXT, text = part.path("text").asText(""))
                }
            } }
        return ChatChoice(0, ChatMessage("assistant", parts), body.path("status").asText("completed"))
    }

    private fun parseOpenAiResponsesUsage(body: JsonNode): Usage? {
        val usage = body.path("usage")
        if (!usage.isObject) return null
        return Usage(usage.path("input_tokens").asLong(0), usage.path("output_tokens").asLong(0))
    }

    private fun parseAnthropicUsage(body: JsonNode): Usage? {
        val usage = body.path("usage")
        if (!usage.isObject) return null
        return Usage(usage.path("input_tokens").asLong(0), usage.path("output_tokens").asLong(0))
    }

    private fun parseGeminiUsage(body: JsonNode): Usage? {
        val usage = body.path("usageMetadata")
        if (!usage.isObject) return null
        return Usage(usage.path("promptTokenCount").asLong(0), usage.path("candidatesTokenCount").asLong(0))
    }

    private fun openAiResponse(response: ChatResponse): JsonNode {
        val body = mapper.createObjectNode()
        body.put("id", response.id); body.put("object", "chat.completion"); body.put("created", response.createdAt); body.put("model", response.model)
        val choices = body.putArray("choices")
        response.choices.forEach { choice ->
            val message = choices.addObject().put("index", choice.index).put("finish_reason", choice.finishReason ?: "stop").putObject("message")
            message.put("role", choice.message.role)
            val text = choice.message.parts.filter { it.type == PartType.TEXT && it.name == null }.joinToString("") { it.text ?: "" }
            message.put("content", text)
            val toolCalls = message.putArray("tool_calls")
            choice.message.parts.filter { it.type == PartType.TEXT && it.name != null }.forEach { call ->
                val item = toolCalls.addObject()
                item.put("id", call.toolCallId ?: UUID.randomUUID().toString()); item.put("type", "function")
                item.putObject("function").put("name", call.name ?: "").put("arguments", call.text ?: "{}")
            }
        }
        response.usage?.let { usage ->
            val usageNode = body.putObject("usage")
            usageNode.put("prompt_tokens", usage.inputTokens); usageNode.put("completion_tokens", usage.outputTokens); usageNode.put("total_tokens", usage.totalTokens)
        }
        return body
    }

    private fun openAiResponsesResponse(response: ChatResponse): JsonNode {
        val body = mapper.createObjectNode()
        body.put("id", response.id)
        body.put("object", "response")
        body.put("created_at", response.createdAt)
        body.put("model", response.model)
        body.put("status", response.choices.firstOrNull()?.finishReason ?: "completed")
        val output = body.putArray("output")
        response.choices.firstOrNull()?.message?.parts?.forEach { part ->
            if (part.name != null) {
                val call = output.addObject().put("type", "function_call")
                call.put("call_id", part.toolCallId ?: UUID.randomUUID().toString())
                call.put("name", part.name ?: "")
                call.put("arguments", part.text ?: "{}")
            } else if (!part.text.isNullOrBlank()) {
                output.addObject()
                    .put("type", "message")
                    .put("id", "msg-${response.id}")
                    .put("role", "assistant")
                    .put("status", "completed")
                    .putArray("content")
                    .addObject()
                    .put("type", "output_text")
                    .put("text", part.text)
            }
        }
        response.usage?.let { usage ->
            body.putObject("usage")
                .put("input_tokens", usage.inputTokens)
                .put("output_tokens", usage.outputTokens)
                .put("total_tokens", usage.totalTokens)
        }
        return body
    }

    private fun anthropicResponse(response: ChatResponse): JsonNode {
        val body = mapper.createObjectNode()
        body.put("id", response.id); body.put("type", "message"); body.put("role", "assistant"); body.put("model", response.model)
        val content = body.putArray("content")
        response.choices.firstOrNull()?.message?.parts?.forEach { part ->
            if (part.name != null) {
                val tool = content.addObject().put("type", "tool_use")
                tool.put("id", part.toolCallId ?: UUID.randomUUID().toString()); tool.put("name", part.name ?: "")
                tool.set("input", runCatching { mapper.readTree(part.text ?: "{}") }.getOrDefault(mapper.createObjectNode()))
            } else if (!part.text.isNullOrBlank()) content.addObject().put("type", "text").put("text", part.text)
        }
        body.put("stop_reason", response.choices.firstOrNull()?.finishReason ?: "end_turn")
        response.usage?.let { body.putObject("usage").put("input_tokens", it.inputTokens).put("output_tokens", it.outputTokens) }
        return body
    }

    private fun geminiResponse(response: ChatResponse): JsonNode {
        val body = mapper.createObjectNode()
        body.put("modelVersion", response.model)
        val candidates = body.putArray("candidates")
        response.choices.forEach { choice ->
            val candidate = candidates.addObject()
            val content = candidate.putObject("content")
            content.put("role", "model")
            val parts = content.putArray("parts")
            choice.message.parts.forEach { part ->
                if (part.name != null) {
                    val call = parts.addObject().putObject("functionCall")
                    call.put("name", part.name ?: "")
                    call.set("args", runCatching { mapper.readTree(part.text ?: "{}") }.getOrDefault(mapper.createObjectNode()))
                } else if (!part.text.isNullOrBlank()) parts.addObject().put("text", part.text)
            }
            candidate.put("finishReason", choice.finishReason ?: "STOP")
        }
        response.usage?.let { usage ->
            val usageNode = body.putObject("usageMetadata")
            usageNode.put("promptTokenCount", usage.inputTokens); usageNode.put("candidatesTokenCount", usage.outputTokens); usageNode.put("totalTokenCount", usage.totalTokens)
        }
        return body
    }
}
