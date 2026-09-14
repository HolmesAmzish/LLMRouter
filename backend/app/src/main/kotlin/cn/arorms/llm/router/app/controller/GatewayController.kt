package cn.arorms.llm.router.app.controller

import cn.arorms.llm.router.common.Protocol
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import cn.arorms.llm.router.app.service.GatewayService
import cn.arorms.llm.router.app.service.ModelService
import cn.arorms.llm.router.common.ModelListResponse
import org.springframework.web.bind.annotation.*

@RestController
class GatewayController(
    private val gatewayService: GatewayService,
    private val modelService: ModelService
) {
    @PostMapping("/v1/chat/completions")
    suspend fun openAi(@RequestBody body: JsonNode): JsonNode = gatewayService.chat(body, Protocol.OPENAI)

    @PostMapping("/v1/messages")
    suspend fun anthropic(@RequestBody body: JsonNode): JsonNode = gatewayService.chat(body, Protocol.ANTHROPIC)

    @PostMapping("/v1beta/models/{model}:generateContent")
    suspend fun gemini(@PathVariable model: String, @RequestBody raw: JsonNode): JsonNode {
        val body = raw.deepCopy<JsonNode>()
        if (body is ObjectNode) body.put("model", model)
        return gatewayService.chat(body, Protocol.GEMINI)
    }

    @GetMapping("/v1/models")
    suspend fun openAiModels(): ModelListResponse = modelService.remoteModels(Protocol.OPENAI)

    @GetMapping("/v1beta/models")
    suspend fun geminiModels(): ModelListResponse = modelService.remoteModels(Protocol.GEMINI)
}
