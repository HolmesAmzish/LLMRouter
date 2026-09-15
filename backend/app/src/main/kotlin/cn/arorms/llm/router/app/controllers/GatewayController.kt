package cn.arorms.llm.router.app.controllers

import cn.arorms.llm.router.app.services.GatewayService
import cn.arorms.llm.router.app.services.ModelService
import cn.arorms.llm.router.common.enums.Protocol
import cn.arorms.llm.router.common.responses.ModelListResponse
import com.fasterxml.jackson.databind.JsonNode
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
    fun openAiChat(@RequestBody body: JsonNode): Any {
        if (body.path("stream").asBoolean(false)) {
            return gatewayService.chatStream(body, Protocol.OPENAI)
        }
        return mono { gatewayService.chat(body, Protocol.OPENAI) }
    }

    @PostMapping("/v1/responses")
    fun openAiResponses(@RequestBody body: JsonNode): Any {
        if (body.path("stream").asBoolean(false)) {
            return gatewayService.chatStream(body, Protocol.OPENAI_RESPONSES)
        }
        return mono { gatewayService.chat(body, Protocol.OPENAI_RESPONSES) }
    }

    @PostMapping("/v1/messages")
    fun anthropic(@RequestBody body: JsonNode): Any {
        if (body.path("stream").asBoolean(false)) {
            return gatewayService.chatStream(body, Protocol.ANTHROPIC)
        }
        return mono { gatewayService.chat(body, Protocol.ANTHROPIC) }
    }

    // Gemini protocol is intentionally disabled for now.
    // @PostMapping("/v1beta/models/{model}:generateContent")
    // fun gemini(@PathVariable model: String, @RequestBody body: JsonNode): Mono<JsonNode> = mono {
    //     gatewayService.chat(withModel(body, model), Protocol.GEMINI)
    // }

    // @PostMapping("/v1beta/models/{model}:streamGenerateContent")
    // fun geminiStream(@PathVariable model: String, @RequestBody body: JsonNode): Flux<ServerSentEvent<String>> {
    //     return gatewayService.chatStream(withModel(body, model), Protocol.GEMINI)
    // }

    @GetMapping("/v1/models")
    suspend fun openAiModels(): ModelListResponse = modelService.remoteModels(Protocol.OPENAI)

    @GetMapping("/v1/openai/responses/models")
    suspend fun openAiResponsesModels(): ModelListResponse = modelService.remoteModels(Protocol.OPENAI_RESPONSES)

    // @GetMapping("/v1beta/models")
    // suspend fun geminiModels(): ModelListResponse = modelService.remoteModels(Protocol.GEMINI)

    // private fun withModel(raw: JsonNode, model: String): JsonNode {
    //     val body = raw.deepCopy<JsonNode>()
    //     if (body is ObjectNode) body.put("model", model)
    //     return body
    // }
}
