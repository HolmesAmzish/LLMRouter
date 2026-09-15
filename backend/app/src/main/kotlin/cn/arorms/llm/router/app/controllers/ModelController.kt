package cn.arorms.llm.router.app.controllers

import cn.arorms.llm.router.app.services.ModelService
import cn.arorms.llm.router.common.enums.Protocol
import cn.arorms.llm.router.common.requests.ManualModelRequest
import cn.arorms.llm.router.common.responses.ModelListResponse
import cn.arorms.llm.router.common.responses.ModelResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/models")
class ModelController(private val service: ModelService) {
    @GetMapping
    fun models(): ModelListResponse = service.localModels()

    @GetMapping("/upstream")
    suspend fun upstream(@RequestParam protocol: Protocol): ModelListResponse = service.remoteModels(protocol)

    @PostMapping
    fun create(@Valid @RequestBody request: ManualModelRequest): ModelResponse = service.create(request)

    @PatchMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestParam enabled: Boolean): ModelResponse = service.setEnabled(id, enabled)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)

    @PostMapping("/accounts/{accountId}/sync")
    suspend fun sync(@PathVariable accountId: Long): ModelListResponse = service.sync(accountId)

    @PostMapping("/accounts/{accountId}/{protocol}/sync")
    suspend fun syncProtocol(@PathVariable accountId: Long, @PathVariable protocol: Protocol): ModelListResponse =
        service.sync(accountId, protocol)
}
