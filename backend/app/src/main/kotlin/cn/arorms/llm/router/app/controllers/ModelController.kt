package cn.arorms.llm.router.app.controllers

import cn.arorms.llm.router.app.services.ModelService
import cn.arorms.llm.router.common.enums.Protocol
import cn.arorms.llm.router.common.responses.ModelListResponse
import cn.arorms.llm.router.common.responses.ProviderModelResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/models")
class ModelController(private val service: ModelService) {
    @GetMapping
    fun models(): ModelListResponse = service.localModels()

    @GetMapping("/upstream")
    suspend fun upstream(@RequestParam protocol: Protocol): ModelListResponse = service.remoteModels(protocol)

    @PatchMapping("/provider-models/{id}")
    fun updateProviderModel(
        @PathVariable id: Long,
        @RequestParam enabled: Boolean
    ): ProviderModelResponse {
        return service.setProviderModelEnabled(id, enabled)
    }

    @DeleteMapping("/provider-models/{id}")
    fun deleteProviderModel(@PathVariable id: Long) = service.deleteProviderModel(id)

    @PostMapping("/accounts/{accountId}/sync")
    suspend fun sync(@PathVariable accountId: Long): ModelListResponse = service.sync(accountId)

    @PostMapping("/accounts/{accountId}/{protocol}/sync")
    suspend fun syncProtocol(@PathVariable accountId: Long, @PathVariable protocol: Protocol): ModelListResponse =
        service.sync(accountId, protocol)
}
