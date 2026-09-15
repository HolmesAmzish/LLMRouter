package cn.arorms.llm.router.app.controllers

import cn.arorms.llm.router.app.services.GatewayService
import cn.arorms.llm.router.app.services.ModelService
import cn.arorms.llm.router.common.enums.Protocol
import cn.arorms.llm.router.common.responses.ModelListResponse
import kotlinx.coroutines.reactor.mono
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.core.Disposable
import reactor.core.publisher.Flux

private val streamRequest = Regex("""\"stream\"\s*:\s*true""", RegexOption.IGNORE_CASE)

@RestController
class GatewayController(
    private val gatewayService: GatewayService,
    private val modelService: ModelService
) {
    @PostMapping("/v1/chat/completions")
    fun openAiChat(@RequestBody body: String): Any {
        if (streamRequest.containsMatchIn(body)) return sse(gatewayService.chatStream(body, Protocol.OPENAI))
        return mono { ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(gatewayService.chat(body, Protocol.OPENAI)) }
    }

    @PostMapping("/v1/responses")
    fun openAiResponses(@RequestBody body: String): Any {
        if (streamRequest.containsMatchIn(body)) return sse(gatewayService.chatStream(body, Protocol.OPENAI_RESPONSES))
        return mono { ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(gatewayService.chat(body, Protocol.OPENAI_RESPONSES)) }
    }

    @PostMapping("/v1/messages")
    fun anthropic(@RequestBody body: String): Any {
        if (streamRequest.containsMatchIn(body)) return sse(gatewayService.chatStream(body, Protocol.ANTHROPIC))
        return mono { ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(gatewayService.chat(body, Protocol.ANTHROPIC)) }
    }

    // Gemini routes are intentionally disabled for now.

    @GetMapping("/v1/models")
    suspend fun openAiModels(): ModelListResponse = modelService.localModels()

    @GetMapping("/v1/openai/responses/models")
    suspend fun openAiResponsesModels(): ModelListResponse = modelService.localModels()

    private fun sse(events: Flux<ServerSentEvent<String>>): SseEmitter {
        val emitter = SseEmitter(0L)
        val subscription: Disposable = events.subscribe(
            { event ->
                val builder = SseEmitter.event().data(event.data() ?: "")
                event.event()?.takeIf { it.isNotBlank() }?.let(builder::name)
                emitter.send(builder)
            },
            emitter::completeWithError,
            emitter::complete
        )
        emitter.onCompletion { subscription.dispose() }
        emitter.onTimeout { subscription.dispose() }
        return emitter
    }
}
