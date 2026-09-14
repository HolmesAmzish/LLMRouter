package cn.arorms.llm.router.app.controllers

import cn.arorms.llm.router.common.enums.Protocol
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import cn.arorms.llm.router.app.services.GatewayService
import cn.arorms.llm.router.app.services.ModelService
import cn.arorms.llm.router.common.responses.ModelListResponse
import kotlinx.coroutines.reactor.mono
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class GatewayController(
    private val gatewayService: GatewayService,
    private val modelService: ModelService
) {
    @PostMapping("/v1/chat/completions")
    fun openAi(@RequestBody body: JsonNode): Any {
        if (body.path("stream").asBoolean(false)) {
            return gatewayService.chatStream(body, Protocol.OPENAI)
        }
        return mono { gatewayService.chat(body, Protocol.OPENAI) }
    }

    @PostMapping("/v1/messages")
    fun anthropic(@RequestBody body: JsonNode): Any {
        if (body.path("stream").asBoolean(false)) {
            return gatewayService.chatStream(body, Protocol.ANTHROPIC)
        }
        return mono { gatewayService.chat(body, Protocol.ANTHROPIC) }
    }

    @PostMapping("/v1beta/models/{model}:generateContent")
    fun gemini(@PathVariable model: String, @RequestBody raw: JsonNode): Mono<JsonNode> = mono {
        gatewayService.chat(withModel(raw, model), Protocol.GEMINI)
    }

    @PostMapping("/v1beta/models/{model}:streamGenerateContent")
    fun geminiStream(@PathVariable model: String, @RequestBody raw: JsonNode): Flux<ServerSentEvent<String>> {
        val body = withModel(raw, model)
        if (body is ObjectNode) body.put("stream", true)
        return gatewayService.chatStream(body, Protocol.GEMINI)
    }

    private fun withModel(raw: JsonNode, model: String): JsonNode {
        val body = raw.deepCopy<JsonNode>()
        if (body is ObjectNode) body.put("model", model)
        return body
    }

    @GetMapping("/v1/models")
    suspend fun openAiModels(): ModelListResponse = modelService.remoteModels(Protocol.OPENAI)

    @GetMapping("/v1beta/models")
    suspend fun geminiModels(): ModelListResponse = modelService.remoteModels(Protocol.GEMINI)
}
