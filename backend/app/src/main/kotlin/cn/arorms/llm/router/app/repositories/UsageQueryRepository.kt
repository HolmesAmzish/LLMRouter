package cn.arorms.llm.router.app.repositories

import cn.arorms.framework.common.domain.QBaseEntity
import cn.arorms.llm.router.app.entities.QUsageRecord
import cn.arorms.llm.router.app.entities.UsageRecord
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class UsageQueryRepository(
    entityManager: EntityManager,
    private val usageRecordRepository: UsageRecordRepository
) {
    private val queryFactory = JPAQueryFactory(entityManager)
    private val usage = QUsageRecord.usageRecord
    private val base = QBaseEntity("usageRecord")

    fun page(
        from: LocalDateTime?,
        to: LocalDateTime?,
        provider: String?,
        model: String?,
        sessionId: String?,
        apiKeyId: Long?,
        requestId: String?,
        page: Int,
        size: Int
    ): Pair<List<UsageRecord>, Long> {
        val predicate = base.createdAt.goe(from ?: LocalDateTime.MIN)
            .and(base.createdAt.loe(to ?: LocalDateTime.MAX))
            .and(provider?.let { usage.provider.eq(it) })
            .and(model?.let { usage.model.eq(it) })
            .and(sessionId?.let { usage.sessionId.eq(it) })
            .and(apiKeyId?.let { usage.apiKeyId.eq(it) })
            .and(requestId?.let { usage.requestId.eq(it) })

        val content = queryFactory.selectFrom(usage)
            .where(predicate)
            .orderBy(base.createdAt.desc())
            .offset(page.toLong() * size)
            .limit(size.toLong())
            .fetch()
        val total = queryFactory.select(usage.count())
            .from(usage)
            .where(predicate)
            .fetchOne() ?: 0L
        return content to total
    }

    fun totalTokens(
        from: LocalDateTime?,
        to: LocalDateTime?,
        provider: String?,
        model: String?,
        sessionId: String?,
        apiKeyId: Long?,
        requestId: String?
    ): Long {
        val predicate = base.createdAt.goe(from ?: LocalDateTime.MIN)
            .and(base.createdAt.loe(to ?: LocalDateTime.MAX))
            .and(provider?.let { usage.provider.eq(it) })
            .and(model?.let { usage.model.eq(it) })
            .and(sessionId?.let { usage.sessionId.eq(it) })
            .and(apiKeyId?.let { usage.apiKeyId.eq(it) })
            .and(requestId?.let { usage.requestId.eq(it) })
        return queryFactory.select(usage.totalTokens.sum())
            .from(usage)
            .where(predicate)
            .fetchOne() ?: 0L
    }
}
