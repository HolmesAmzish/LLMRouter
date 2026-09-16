package cn.arorms.llm.router.app.repositories

import cn.arorms.framework.common.domain.QBaseEntity
import cn.arorms.llm.router.app.entities.QUsageRecord
import cn.arorms.llm.router.app.entities.UsageRecord
import com.querydsl.core.BooleanBuilder
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
        accountName: String?,
        model: String?,
        sessionId: String?,
        apiKeyId: Long?,
        requestId: String?,
        page: Int,
        size: Int
    ): Pair<List<UsageRecord>, Long> {
        val predicate = predicate(from, to, provider, accountName, model, sessionId, apiKeyId, requestId)
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
        accountName: String?,
        model: String?,
        sessionId: String?,
        apiKeyId: Long?,
        requestId: String?
    ): Long = queryFactory.select(usage.totalTokens.sum())
        .from(usage)
        .where(predicate(from, to, provider, accountName, model, sessionId, apiKeyId, requestId))
        .fetchOne() ?: 0L

    private fun predicate(
        from: LocalDateTime?,
        to: LocalDateTime?,
        provider: String?,
        accountName: String?,
        model: String?,
        sessionId: String?,
        apiKeyId: Long?,
        requestId: String?
    ): BooleanBuilder = BooleanBuilder().apply {
        from?.let { and(base.createdAt.goe(it)) }
        to?.let { and(base.createdAt.loe(it)) }
        provider?.let { and(usage.provider.eq(it)) }
        accountName?.let { and(usage.accountName.eq(it)) }
        model?.let { and(usage.model.eq(it)) }
        sessionId?.let { and(usage.sessionId.eq(it)) }
        apiKeyId?.let { and(usage.apiKeyId.eq(it)) }
        requestId?.let { and(usage.requestId.eq(it)) }
    }
}
