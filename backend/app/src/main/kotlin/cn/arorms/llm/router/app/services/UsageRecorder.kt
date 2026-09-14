package cn.arorms.llm.router.app.services

import cn.arorms.llm.router.app.entities.ProviderAccount
import cn.arorms.llm.router.app.entities.Session
import cn.arorms.llm.router.app.entities.UsageRecord
import cn.arorms.llm.router.app.repositories.SessionRepository
import cn.arorms.llm.router.app.repositories.UsageRecordRepository
import cn.arorms.llm.router.common.responses.ChatResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UsageRecorder(
    private val usageRepository: UsageRecordRepository,
    private val sessionRepository: SessionRepository
) {
    @Transactional
    fun record(sessionId: String?, account: ProviderAccount, requestedModel: String, response: ChatResponse, startedAt: Long, status: String) {
        usageRepository.save(
            UsageRecord(
                sessionId = sessionId,
                provider = account.protocol.name,
                accountName = account.name,
                model = requestedModel,
                protocol = account.protocol,
                inputTokens = response.usage?.inputTokens ?: 0,
                outputTokens = response.usage?.outputTokens ?: 0,
                totalTokens = response.usage?.totalTokens ?: 0,
                costCents = response.usage?.costCents,
                latencyMs = (System.nanoTime() - startedAt) / 1_000_000,
                status = status
            )
        )
    }

    @Transactional
    fun updateSession(sessionId: String?, response: ChatResponse) {
        sessionId ?: return
        val session = sessionRepository.findBySessionId(sessionId) ?: return
        session.messageCount += response.choices.size
        session.inputTokens += response.usage?.inputTokens ?: 0
        session.outputTokens += response.usage?.outputTokens ?: 0
    }
}
