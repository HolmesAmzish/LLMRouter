package cn.arorms.llm.router.app.controllers

import cn.arorms.llm.router.app.services.ModelService
import cn.arorms.llm.router.common.responses.ModelListResponse
import cn.arorms.llm.router.common.enums.Protocol
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/models")
class ModelController(private val service: ModelService) {
    @GetMapping
    suspend fun models(@RequestParam(required = false) protocol: Protocol?): ModelListResponse =
        if (protocol == null) service.localModels() else service.remoteModels(protocol)

    @PostMapping("/accounts/{accountId}/sync")
    suspend fun sync(@PathVariable accountId: Long): ModelListResponse = service.sync(accountId)
}
