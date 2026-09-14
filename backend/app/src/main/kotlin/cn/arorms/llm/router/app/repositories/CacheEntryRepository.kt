package cn.arorms.llm.router.app.repositories

import cn.arorms.llm.router.app.entities.CacheEntry
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime

interface CacheEntryRepository : JpaRepository<CacheEntry, Long> {
    fun findFirstByFingerprintAndExpiresAtAfterOrderByUpdatedAtDesc(
        fingerprint: String,
        now: OffsetDateTime
    ): CacheEntry?

    fun deleteByExpiresAtBefore(now: OffsetDateTime)
}
