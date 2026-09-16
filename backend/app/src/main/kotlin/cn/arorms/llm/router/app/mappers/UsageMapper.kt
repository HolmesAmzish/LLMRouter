package cn.arorms.llm.router.app.mappers

import cn.arorms.llm.router.app.entities.UsageRecord
import cn.arorms.llm.router.common.enums.TokenInputSemantics
import cn.arorms.llm.router.common.enums.UsageDataSource
import cn.arorms.llm.router.common.responses.UsageResponse

/**
 * Converts usage persistence objects into API responses.
 */
object UsageMapper {
    fun toResponse(record: UsageRecord): UsageResponse {
        val inputTokens = record.inputTokens
        val outputTokens = record.outputTokens
        val cacheReadTokens = record.cacheReadTokens ?: 0L
        val cacheCreationTokens = record.cacheCreationTokens ?: 0L
        val inputTokenSemantics = record.inputTokenSemantics ?: TokenInputSemantics.UNKNOWN
        val freshInputTokens = when (inputTokenSemantics) {
            TokenInputSemantics.TOTAL -> (inputTokens - cacheReadTokens).coerceAtLeast(0L)
            else -> inputTokens
        }
        return UsageResponse(
            id = record.id ?: 0L,
            requestId = record.requestId ?: "",
            sessionId = record.sessionId,
            apiKeyId = record.apiKeyId,
            apiKeyName = record.apiKeyName,
            provider = record.provider,
            accountName = record.accountName,
            model = record.model,
            publicModel = record.publicModel ?: record.model,
            upstreamModel = record.upstreamModel ?: record.model,
            protocol = record.protocol,
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            totalTokens = record.totalTokens,
            cacheReadTokens = cacheReadTokens,
            cacheCreationTokens = cacheCreationTokens,
            reasoningTokens = record.reasoningTokens,
            realTotalTokens = freshInputTokens + outputTokens + cacheReadTokens + cacheCreationTokens,
            inputTokenSemantics = inputTokenSemantics,
            isStreaming = record.isStreaming ?: false,
            latencyMs = record.latencyMs,
            firstTokenMs = record.firstTokenMs,
            apiBase = record.apiBase,
            cacheHit = record.cacheHit ?: false,
            dataSource = record.dataSource ?: UsageDataSource.UPSTREAM,
            status = record.status,
            statusCode = record.statusCode,
            createdAt = record.createdAt?.toString()
        )
    }
}
