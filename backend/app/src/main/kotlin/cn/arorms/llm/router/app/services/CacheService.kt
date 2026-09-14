package cn.arorms.llm.router.app.services

import cn.arorms.llm.router.app.entities.CacheEntry
import cn.arorms.llm.router.app.repositories.CacheEntryRepository
import cn.arorms.llm.router.common.enums.Protocol
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.OffsetDateTime

@Service
class CacheService(
    private val repository: CacheEntryRepository,
    private val mapper: ObjectMapper,
    @Value("\${application.cache.enabled:true}") private val enabled: Boolean,
    @Value("\${application.cache.ttl-seconds:3600}") private val ttlSeconds: Long
) {
    @Transactional
    fun get(fingerprint: String): String? {
        if (!enabled) return null
        return repository.findFirstByFingerprintAndExpiresAtAfterOrderByUpdatedAtDesc(fingerprint, OffsetDateTime.now())?.let {
            it.hitCount += 1
            it.responseJson
        }
    }

    @Transactional
    fun put(fingerprint: String, responseJson: String, sessionId: String?, protocol: Protocol, model: String) {
        if (!enabled) return
        repository.deleteByExpiresAtBefore(OffsetDateTime.now())
        repository.save(CacheEntry(fingerprint, sessionId, protocol, model, responseJson, 0, OffsetDateTime.now().plusSeconds(ttlSeconds)))
    }

    companion object {
        fun fingerprint(vararg components: String): String = MessageDigest.getInstance("SHA-256")
            .digest(components.joinToString("\u0000").toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
