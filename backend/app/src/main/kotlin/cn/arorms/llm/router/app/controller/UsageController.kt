package cn.arorms.llm.router.app.controller

import cn.arorms.llm.router.app.service.UsageService
import cn.arorms.llm.router.common.SessionResponse
import cn.arorms.llm.router.common.UsageFilter
import cn.arorms.llm.router.common.UsagePageResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/usage")
class UsageController(private val service: UsageService) {
    @GetMapping
    fun usage(@RequestParam from: String?, @RequestParam to: String?, @RequestParam provider: String?,
              @RequestParam model: String?, @RequestParam sessionId: String?,
              @RequestParam page: Int?, @RequestParam size: Int?): UsagePageResponse =
        service.list(UsageFilter(from, to, provider, model, sessionId, page ?: 0, size ?: 20))

    @GetMapping("/sessions/{sessionId}")
    fun session(@PathVariable sessionId: String): SessionResponse = service.sessionStats(sessionId)
}
