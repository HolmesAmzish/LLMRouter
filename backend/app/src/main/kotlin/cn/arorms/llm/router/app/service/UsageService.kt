package cn.arorms.llm.router.app.service

import cn.arorms.llm.router.app.dto.Mappers
import cn.arorms.llm.router.app.repository.SessionRepository
import cn.arorms.llm.router.app.repository.UsageQueryRepository
import cn.arorms.llm.router.common.UsageFilter
import cn.arorms.llm.router.common.UsagePageResponse
import cn.arorms.llm.router.common.SessionResponse
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
        val (records, total) = queryRepository.page(from, to, filter.provider, filter.model, filter.sessionId, filter.page, filter.size)
        return UsagePageResponse(
            content = records.map { it.let(Mappers::toResponse) },
            total = total,
            page = filter.page,
            size = filter.size,
            totalTokens = queryRepository.totalTokens(from, to, filter.provider, filter.model, filter.sessionId)
        )
    }

    @Transactional
    fun sessionStats(sessionId: String): SessionResponse = sessionRepository.findBySessionId(sessionId).orThrow().let { session ->
        SessionResponse(
            id = session.sessionId,
            title = session.title,
            model = session.model,
            protocol = session.protocol,
            enabled = session.enabled,
            messageCount = session.messageCount,
            inputTokens = session.inputTokens,
            outputTokens = session.outputTokens,
            createdAt = session.createdAt?.toString(),
            updatedAt = session.updatedAt?.toString()
        )
    }

    private fun <T> T?.orThrow(): T = this ?: throw NoSuchElementException("Session not found")
}
