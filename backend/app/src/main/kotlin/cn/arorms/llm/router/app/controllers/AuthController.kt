package cn.arorms.llm.router.app.controllers

import cn.arorms.framework.security.UserPrincipal
import cn.arorms.llm.router.common.responses.AuthMeResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {
    @GetMapping("/me")
    fun me(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<AuthMeResponse> = ResponseEntity.ok(
        AuthMeResponse(
            id = principal.id,
            username = principal.username,
            email = principal.email
        )
    )
}
