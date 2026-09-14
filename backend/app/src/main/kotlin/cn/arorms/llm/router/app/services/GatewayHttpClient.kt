package cn.arorms.llm.router.app.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Component
class GatewayHttpClient(
    @Value("\${application.gateway.connect-timeout-ms:5000}") connectTimeoutMs: Long,
    @Value("\${application.gateway.request-timeout-ms:120000}") requestTimeoutMs: Long
) {
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(connectTimeoutMs))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    private val requestTimeout = Duration.ofMillis(requestTimeoutMs)

    suspend fun postJson(url: String, headers: Map<String, String>, body: String): String = execute(
        HttpRequest.newBuilder().uri(URI.create(url)).timeout(requestTimeout)
            .header("content-type", "application/json")
            .apply { headers.forEach { (key, value) -> header(key, value) } }
            .POST(HttpRequest.BodyPublishers.ofString(body)).build()
    )

    suspend fun getJson(url: String, headers: Map<String, String>): String = execute(
        HttpRequest.newBuilder().uri(URI.create(url)).timeout(requestTimeout).GET()
            .apply { headers.forEach { (key, value) -> header(key, value) } }.build()
    )

    private suspend fun execute(request: HttpRequest): String = withContext(Dispatchers.IO) {
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, response.body().ifBlank { "Upstream returned ${response.statusCode()}" })
        }
        response.body()
    }
}
