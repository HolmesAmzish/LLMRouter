package cn.arorms.llm.router.app.services

import cn.arorms.llm.router.app.mappers.SessionMapper
import cn.arorms.llm.router.app.mappers.UsageMapper
import cn.arorms.llm.router.app.repositories.SessionRepository
import cn.arorms.llm.router.app.repositories.UsageQueryRepository
import cn.arorms.llm.router.common.requests.UsageFilter
import cn.arorms.llm.router.common.responses.SessionResponse
import cn.arorms.llm.router.common.responses.UsagePageResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class UsageService(
    private val queryRepository: UsageQueryRepository,
    private val sessionRepository: SessionRepository
) {
    @Transactional
    fun list(filter: UsageFilter): UsagePageResponse {
        val time = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val from = filter.from?.let { LocalDateTime.parse(it, time) }
        val to = filter.to?.let { LocalDateTime.parse(it, time) }
        val (records, total) = queryRepository.page(
            from, to, filter.provider, filter.model, filter.sessionId, filter.page, filter.size
        )
        return UsagePageResponse(
            content = records.map(UsageMapper::toResponse),
            total = total,
            page = filter.page,
            size = filter.size,
            totalTokens = queryRepository.totalTokens(from, to, filter.provider, filter.model, filter.sessionId)
        )
    }

    @Transactional
    fun sessionStats(sessionId: String): SessionResponse = sessionRepository.findBySessionId(sessionId)
        .orThrow()
        .let(SessionMapper::toResponse)

    private fun <T> T?.orThrow(): T = this ?: throw NoSuchElementException("Session not found")
}
