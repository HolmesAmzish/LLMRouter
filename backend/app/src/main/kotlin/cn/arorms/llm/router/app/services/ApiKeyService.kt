package cn.arorms.llm.router.app.services

import cn.arorms.llm.router.app.entities.ApiKey
import cn.arorms.llm.router.app.repositories.ApiKeyRepository
import cn.arorms.llm.router.common.requests.ApiKeyRequest
import cn.arorms.llm.router.common.responses.ApiKeyResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.util.Base64

@Service
class ApiKeyService(private val repository: ApiKeyRepository) {
    private val secureRandom = SecureRandom()

    @Transactional
    fun create(request: ApiKeyRequest): ApiKeyResponse {
        val rawKey = generateKey()
        val entity = ApiKey(
            name = request.name.trim(),
            keyHash = hash(rawKey),
            prefix = rawKey.take(16),
            enabled = true,
            expiresAt = request.expiresAt,
            maxBudget = request.maxBudget?.toBigDecimal(),
            rpmLimit = request.rpmLimit,
            tpmLimit = request.tpmLimit,
            models = request.models,
            metadata = request.metadata
        )
        val saved = repository.save(entity)
        return toResponse(saved, rawKey)
    }

    @Transactional
    fun list(): List<ApiKeyResponse> = repository.findAllByOrderByIdDesc().map { toResponse(it) }

    @Transactional
    fun revoke(id: Long): ApiKeyResponse {
        val key = repository.findById(id).orElseThrow()
        key.enabled = false
        return toResponse(key)
    }

    @Transactional
    fun delete(id: Long) = repository.deleteById(id)

    @Transactional
    fun requireForPlayground(id: Long): ApiKey {
        val key = repository.findById(id).orElseThrow()
        val now = OffsetDateTime.now()
        if (!key.enabled || key.expiresAt?.isBefore(now) == true) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "API key is disabled or expired")
        }
        if (key.maxBudget != null && (key.spend ?: BigDecimal.ZERO) >= key.maxBudget) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "API key budget exceeded")
        }
        key.lastUsedAt = now
        return key
    }

    @Transactional
    fun authenticate(rawKey: String): ApiKey? {
        val key = repository.findByKeyHash(hash(rawKey)) ?: return null
        val now = OffsetDateTime.now()
        if (!key.enabled || key.expiresAt?.isBefore(now) == true) return null
        if (key.maxBudget != null && (key.spend ?: BigDecimal.ZERO) >= key.maxBudget) return null
        key.lastUsedAt = now
        return key
    }

    private fun generateKey(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return "sk-router-" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hash(rawKey: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(rawKey.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun toResponse(key: ApiKey, rawKey: String? = null) = ApiKeyResponse(
        id = key.id ?: 0L,
        name = key.name,
        prefix = key.prefix,
        apiKey = rawKey,
        keyType = key.keyType ?: cn.arorms.llm.router.common.enums.ApiKeyType.GATEWAY,
        enabled = key.enabled,
        expiresAt = key.expiresAt,
        lastUsedAt = key.lastUsedAt,
        maxBudget = key.maxBudget,
        spend = key.spend ?: BigDecimal.ZERO,
        rpmLimit = key.rpmLimit,
        tpmLimit = key.tpmLimit,
        models = key.models ?: emptySet(),
        metadata = key.metadata ?: emptyMap(),
        createdAt = key.createdAt?.toString(),
        updatedAt = key.updatedAt?.toString()
    )
}
