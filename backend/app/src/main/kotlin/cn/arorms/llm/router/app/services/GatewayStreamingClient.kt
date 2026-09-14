package cn.arorms.llm.router.app.services

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Component
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import io.netty.channel.ChannelOption
import reactor.core.publisher.Flux
import reactor.netty.http.client.HttpClient
import java.time.Duration

@Component
class GatewayStreamingClient(
    builder: WebClient.Builder,
    @Value("\${application.gateway.connect-timeout-ms:5000}") connectTimeoutMs: Long,
    @Value("\${application.gateway.request-timeout-ms:120000}") requestTimeoutMs: Long
) {
    private val timeout = Duration.ofMillis(requestTimeoutMs)

    private val webClient = builder
        .clone()
        .clientConnector(
            ReactorClientHttpConnector(
                HttpClient.create()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs.toInt())
                    .responseTimeout(timeout)
            )
        )
        .build()

    init {
        require(connectTimeoutMs > 0)
    }

    fun stream(
        url: String,
        headers: Map<String, String>,
        body: String
    ): Flux<ServerSentEvent<String>> = webClient.post()
        .uri(url)
        .headers { httpHeaders ->
            httpHeaders.contentType = MediaType.TEXT_EVENT_STREAM
            headers.forEach { (name, value) -> httpHeaders.set(name, value) }
        }
        .bodyValue(body)
        .accept(MediaType.TEXT_EVENT_STREAM)
        .retrieve()
        .bodyToFlux(object : ParameterizedTypeReference<ServerSentEvent<String>>() {})
        .timeout(timeout)
}
