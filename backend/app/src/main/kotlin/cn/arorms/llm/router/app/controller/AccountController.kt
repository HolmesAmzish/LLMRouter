package cn.arorms.llm.router.app.controller

import cn.arorms.llm.router.app.service.AccountService
import cn.arorms.llm.router.common.AccountBalanceResponse
import cn.arorms.llm.router.common.Protocol
import cn.arorms.llm.router.common.ProviderAccountPatch
import cn.arorms.llm.router.common.ProviderAccountRequest
import cn.arorms.llm.router.common.ProviderAccountResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/accounts")
class AccountController(private val service: AccountService) {
    @GetMapping
    fun list(@RequestParam provider: Protocol?, @RequestParam enabled: Boolean?): List<ProviderAccountResponse> =
        service.list(provider?.name, enabled)

    @PostMapping
    fun create(@Valid @RequestBody request: ProviderAccountRequest): ProviderAccountResponse = service.create(request)

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ProviderAccountResponse = service.get(id)

    @PatchMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody request: ProviderAccountPatch): ProviderAccountResponse = service.update(id, request)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)

    @PostMapping("/{id}/balance/refresh")
    suspend fun balance(@PathVariable id: Long): AccountBalanceResponse = service.refreshBalance(id)
}
