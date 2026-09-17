package cn.arorms.llm.router.app.controllers

import cn.arorms.llm.router.app.services.ApiKeyService
import cn.arorms.llm.router.app.services.GatewayService
import cn.arorms.llm.router.app.services.WebChatService
import cn.arorms.llm.router.common.enums.Protocol
import cn.arorms.llm.router.common.requests.WebChatRequest
import cn.arorms.llm.router.common.responses.WebChatMessageResponse
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import reactor.core.Disposable
import reactor.core.publisher.Flux

/**
 * Authenticated web-chat proxy. The browser selects an API key ID; history is
 * loaded from chat_messages on the server and never trusted from the client.
 */
@RestController
@RequestMapping("/api/v1/playground")
class PlaygroundController(
    private val gatewayService: GatewayService,
    private val apiKeyService: ApiKeyService,
    private val webChatService: WebChatService
) {
    @GetMapping("/history")
    fun history(): List<WebChatMessageResponse> = webChatService.history()

    @DeleteMapping("/history")
    fun clear() = webChatService.clear()

    @PostMapping("/chat/completions")
    fun openAiChat(@RequestParam apiKeyId: Long, @RequestBody request: WebChatRequest) =
        stream(apiKeyId, request.copy(protocol = Protocol.OPENAI), Protocol.OPENAI)

    @PostMapping("/responses")
    fun openAiResponses(@RequestParam apiKeyId: Long, @RequestBody request: WebChatRequest) =
        stream(apiKeyId, request.copy(protocol = Protocol.OPENAI_RESPONSES), Protocol.OPENAI_RESPONSES)

    @PostMapping("/messages")
    fun anthropic(@RequestParam apiKeyId: Long, @RequestBody request: WebChatRequest) =
        stream(apiKeyId, request.copy(protocol = Protocol.ANTHROPIC), Protocol.ANTHROPIC)

    private fun stream(apiKeyId: Long, request: WebChatRequest, protocol: Protocol): SseEmitter {
        val apiKey = apiKeyService.requireForPlayground(apiKeyId)
        val history = webChatService.appendUser(request)
        val body = webChatService.buildGatewayBody(request, history)
        val assistant = StringBuilder()

        val events = gatewayService.chatStream(body, protocol, apiKey)
            .doOnNext { event ->
                assistant.append(webChatService.extractDelta(protocol, event.data()))
            }
            .doOnComplete {
                webChatService.appendAssistant(assistant.toString(), protocol, request.model)
            }
            .doOnError { error ->
                webChatService.appendFailed(
                    protocol = protocol,
                    model = request.model,
                    error = error.message ?: error.javaClass.simpleName,
                    partialContent = assistant.toString()
                )
            }

        return sse(events)
    }

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
