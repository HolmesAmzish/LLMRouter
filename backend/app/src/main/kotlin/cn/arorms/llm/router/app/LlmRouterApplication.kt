package cn.arorms.llm.router.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LlmRouterApplication

fun main(args: Array<String>) {
    runApplication<LlmRouterApplication>(*args)
}
