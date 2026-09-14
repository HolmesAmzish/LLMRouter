package cn.arorms.llm.router.app

import cn.arorms.llm.router.app.adapter.Adapters
import cn.arorms.llm.router.common.Protocol
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.test.Test
import kotlin.test.assertEquals

class AdaptersTest {
    private val mapper = jacksonObjectMapper()

    @Test
    fun `openai image request converts to anthropic image block`() {
        val openAi = mapper.readTree("""
            {"model":"gpt-4o","messages":[{"role":"user","content":[{"type":"text","text":"What is this?"},{"type":"image_url","image_url":{"url":"data:image/png;base64,abc"}}]}],"max_tokens":100}
        """.trimIndent())
        val canonical = Adapters.parseRequest(openAi, Protocol.OPENAI)
        val outbound = Adapters.toOutboundRequest(canonical, Protocol.ANTHROPIC)
        assertEquals("image", outbound.path("messages").get(0).path("content").get(1).path("type").asText())
        assertEquals("abc", outbound.path("messages").get(0).path("content").get(1).path("source").path("data").asText())
    }

    @Test
    fun `anthropic request converts to gemini`() {
        val anthropic = mapper.readTree("""
            {"model":"claude-3-5-sonnet","system":"You are helpful","max_tokens":256,"messages":[{"role":"user","content":"Hello"}]}
        """.trimIndent())
        val canonical = Adapters.parseRequest(anthropic, Protocol.ANTHROPIC)
        val outbound = Adapters.toOutboundRequest(canonical, Protocol.GEMINI)
        assertEquals("You are helpful", outbound.path("systemInstruction").path("parts").get(0).path("text").asText())
        assertEquals("Hello", outbound.path("contents").get(0).path("parts").get(0).path("text").asText())
    }
}
