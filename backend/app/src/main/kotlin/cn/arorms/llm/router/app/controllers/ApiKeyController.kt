package cn.arorms.llm.router.app.controllers

import cn.arorms.llm.router.app.services.ApiKeyService
import cn.arorms.llm.router.common.requests.ApiKeyRequest
import cn.arorms.llm.router.common.responses.ApiKeyResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/api-keys")
class ApiKeyController(private val service: ApiKeyService) {
    @GetMapping
    fun list(): List<ApiKeyResponse> = service.list()

    @PostMapping
    fun create(@RequestBody request: ApiKeyRequest): ApiKeyResponse = service.create(request)

    @PostMapping("/{id}/revoke")
    fun revoke(@PathVariable id: Long): ApiKeyResponse = service.revoke(id)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)
}
