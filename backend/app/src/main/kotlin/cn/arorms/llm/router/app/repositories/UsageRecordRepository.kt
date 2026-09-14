package cn.arorms.llm.router.app.repositories

import cn.arorms.llm.router.app.entities.UsageRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface UsageRecordRepository : JpaRepository<UsageRecord, Long> {
    fun findBySessionIdOrderByCreatedAtDesc(sessionId: String): List<UsageRecord>

    @Query(
        """
        select sum(u.totalTokens) from UsageRecord u
        where (:sessionId is null or u.sessionId = :sessionId)
          and (:provider is null or u.provider = :provider)
          and (:model is null or u.model = :model)
          and (:from is null or u.createdAt >= :from)
          and (:to is null or u.createdAt <= :to)
        """
    )
    fun totalTokens(
        @Param("from") from: LocalDateTime?,
        @Param("to") to: LocalDateTime?,
        @Param("provider") provider: String?,
        @Param("model") model: String?,
        @Param("sessionId") sessionId: String?
    ): Long?
}
