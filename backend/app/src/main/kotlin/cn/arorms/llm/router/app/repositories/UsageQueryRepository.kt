package cn.arorms.llm.router.app.repositories

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

    fun page(
        from: LocalDateTime?,
        to: LocalDateTime?,
        provider: String?,
        model: String?,
        sessionId: String?,
        page: Int,
        size: Int
    ): Pair<List<UsageRecord>, Long> {
        val predicate = usage.createdAt.goe(from ?: LocalDateTime.MIN)
            .and(usage.createdAt.loe(to ?: LocalDateTime.MAX))
            .and(provider?.let { usage.provider.eq(it) })
            .and(model?.let { usage.model.eq(it) })
            .and(sessionId?.let { usage.sessionId.eq(it) })

        val content = queryFactory.selectFrom(usage)
            .where(predicate)
            .orderBy(usage.createdAt.desc())
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
        sessionId: String?
    ): Long = usageRecordRepository.totalTokens(from, to, provider, model, sessionId) ?: 0L
}
