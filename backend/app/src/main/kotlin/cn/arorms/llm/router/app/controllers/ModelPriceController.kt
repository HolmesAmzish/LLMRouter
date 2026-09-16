package cn.arorms.llm.router.app.controllers

import cn.arorms.llm.router.app.services.ModelService
import cn.arorms.llm.router.common.requests.ModelPricePatch
import cn.arorms.llm.router.common.requests.ModelPriceRequest
import cn.arorms.llm.router.common.responses.ModelPriceResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/models/prices")
class ModelPriceController(private val service: ModelService) {
    @GetMapping
    fun list(): List<ModelPriceResponse> = service.listPrices()

    @PostMapping
    fun create(@RequestBody request: ModelPriceRequest): ModelPriceResponse = service.createPrice(request)

    @PatchMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody request: ModelPricePatch): ModelPriceResponse =
        service.updatePrice(id, request)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) = service.deletePrice(id)
}
