package cn.arorms.llm.router.app.controllers

import cn.arorms.llm.router.app.services.UsageService
import cn.arorms.llm.router.common.responses.SessionResponse
import cn.arorms.llm.router.common.requests.UsageFilter
import cn.arorms.llm.router.common.responses.UsagePageResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/usage")
class UsageController(private val service: UsageService) {
    @GetMapping
    fun usage(@RequestParam from: String?, @RequestParam to: String?, @RequestParam provider: String?,
              @RequestParam model: String?, @RequestParam sessionId: String?,
              @RequestParam page: Int?, @RequestParam size: Int?): UsagePageResponse =
        service.list(
            UsageFilter(
                from = from,
                to = to,
                provider = provider,
                model = model,
                sessionId = sessionId,
                page = page ?: 0,
                size = size ?: 20
            )
        )

    @GetMapping("/sessions/{sessionId}")
    fun session(@PathVariable sessionId: String): SessionResponse = service.sessionStats(sessionId)
}
