package cn.arorms.llm.router.app.repository

import cn.arorms.llm.router.app.entity.*
import cn.arorms.llm.router.common.Protocol
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.time.LocalDateTime

interface ProviderAccountRepository : JpaRepository<ProviderAccountEntity, Long> {
    fun findByEnabledTrueOrderByPriorityAscIdAsc(): List<ProviderAccountEntity>
}

interface SessionRepository : JpaRepository<SessionEntity, Long> {
    fun findBySessionId(sessionId: String): SessionEntity?
}

interface UsageRecordRepository : JpaRepository<UsageRecordEntity, Long> {
    fun findBySessionIdOrderByCreatedAtDesc(sessionId: String): List<UsageRecordEntity>

    @Query("""
        select sum(u.totalTokens) from UsageRecordEntity u
        where (:sessionId is null or u.sessionId = :sessionId)
          and (:provider is null or u.provider = :provider)
          and (:model is null or u.model = :model)
          and (:from is null or u.createdAt >= :from)
          and (:to is null or u.createdAt <= :to)
    """)
    fun totalTokens(
        @Param("from") from: LocalDateTime?,
        @Param("to") to: LocalDateTime?,
        @Param("provider") provider: String?,
        @Param("model") model: String?,
        @Param("sessionId") sessionId: String?
    ): Long?
}

interface CacheEntryRepository : JpaRepository<CacheEntryEntity, Long> {
    fun findFirstByFingerprintAndExpiresAtAfterOrderByUpdatedAtDesc(fingerprint: String, now: OffsetDateTime): CacheEntryEntity?
    fun deleteByExpiresAtBefore(now: OffsetDateTime)
}

interface ModelCatalogRepository : JpaRepository<ModelCatalogEntity, Long> {
    fun findByAccountId(accountId: Long): List<ModelCatalogEntity>
    fun deleteByAccountId(accountId: Long)
    fun findByEnabledTrue(): List<ModelCatalogEntity>
}
