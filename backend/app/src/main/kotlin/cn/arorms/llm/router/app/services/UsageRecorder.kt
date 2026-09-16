package cn.arorms.llm.router.app.services

import cn.arorms.llm.router.app.entities.ApiKey
import cn.arorms.llm.router.app.entities.ProviderAccount
import cn.arorms.llm.router.app.entities.UsageRecord
import cn.arorms.llm.router.app.repositories.ApiKeyRepository
import cn.arorms.llm.router.app.repositories.SessionRepository
import cn.arorms.llm.router.app.repositories.UsageRecordRepository
import cn.arorms.llm.router.common.enums.Protocol
import cn.arorms.llm.router.common.enums.UsageDataSource
import cn.arorms.llm.router.common.responses.ChatResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime
import java.util.UUID

@Service
class UsageRecorder(
    private val usageRepository: UsageRecordRepository,
    private val sessionRepository: SessionRepository,
    private val apiKeyRepository: ApiKeyRepository
) {
    @Transactional
    fun record(
        sessionId: String?,
        account: ProviderAccount,
        providerName: String,
        requestedModel: String,
        publicModel: String,
        upstreamModel: String,
        protocol: Protocol,
        response: ChatResponse,
        startedAt: Long,
        status: String,
        apiKey: ApiKey? = null,
        apiBase: String? = null,
        cacheHit: Boolean = false,
        cacheKey: String? = null,
        statusCode: Int? = null,
        firstTokenAt: Long? = null,
        isStreaming: Boolean = false,
        dataSource: UsageDataSource = UsageDataSource.UPSTREAM,
        errorMessage: String? = null
    ): UsageRecord {
        val record = usageRepository.save(
            UsageRecord(
                requestId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                apiKeyId = apiKey?.id,
                apiKeyName = apiKey?.name,
                provider = providerName,
                accountName = account.name,
                model = requestedModel,
                publicModel = publicModel,
                upstreamModel = upstreamModel,
                protocol = protocol,
                inputTokens = response.usage?.inputTokens ?: 0L,
                outputTokens = response.usage?.outputTokens ?: 0L,
                totalTokens = response.usage?.totalTokens ?: 0L,
                cacheReadTokens = response.usage?.cacheReadTokens ?: 0L,
                cacheCreationTokens = response.usage?.cacheCreationTokens ?: 0L,
                reasoningTokens = response.usage?.reasoningTokens,
                inputTokenSemantics = response.usage?.inputTokenSemantics,
                tokenDetails = response.usage?.tokenDetails,
                latencyMs = (System.nanoTime() - startedAt) / 1_000_000,
                firstTokenMs = firstTokenAt?.let { (it - startedAt) / 1_000_000 },
                apiBase = apiBase,
                cacheHit = cacheHit,
                cacheKey = cacheKey,
                dataSource = dataSource,
                isStreaming = isStreaming,
                status = status,
                statusCode = statusCode,
                errorMessage = errorMessage,
                startedAt = startedNanoToOffset(startedAt),
                finishedAt = OffsetDateTime.now()
            )
        )
        apiKey?.id?.let { keyId ->
            apiKeyRepository.findById(keyId).ifPresent { persisted ->
                val spendDelta = response.usage?.costCents
                    ?.toBigDecimal()
                    ?.divide(BigDecimal(100), 8, RoundingMode.HALF_UP)
                    ?: BigDecimal.ZERO
                persisted.spend = (persisted.spend ?: BigDecimal.ZERO) + spendDelta
            }
        }
        return record
    }

    @Transactional
    fun updateSession(sessionId: String?, response: ChatResponse) {
        sessionId ?: return
        val session = sessionRepository.findBySessionId(sessionId) ?: return
        session.messageCount += response.choices.size
        session.inputTokens += response.usage?.inputTokens ?: 0L
        session.outputTokens += response.usage?.outputTokens ?: 0L
    }

    private fun startedNanoToOffset(startedAt: Long): OffsetDateTime =
        OffsetDateTime.now().minusNanos(System.nanoTime() - startedAt)
}
