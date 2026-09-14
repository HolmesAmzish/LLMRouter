package cn.arorms.llm.router.app.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class HealthController {
    @GetMapping("/actuator/health")
    fun health(): Map<String, String> = mapOf("status" to "UP", "time" to Instant.now().toString())
}
