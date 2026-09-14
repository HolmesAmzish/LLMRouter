package cn.arorms.llm.router.app.controller

import cn.arorms.llm.router.app.service.SessionService
import cn.arorms.llm.router.common.SessionRequest
import cn.arorms.llm.router.common.SessionResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/sessions")
class SessionController(private val service: SessionService) {
    @GetMapping
    fun list(): List<SessionResponse> = service.list()

    @PostMapping
    fun create(@Valid @RequestBody request: SessionRequest): SessionResponse = service.create(request)

    @GetMapping("/{sessionId}")
    fun get(@PathVariable sessionId: String): SessionResponse = service.get(sessionId)

    @DeleteMapping("/{sessionId}")
    fun delete(@PathVariable sessionId: String) = service.delete(sessionId)
}
