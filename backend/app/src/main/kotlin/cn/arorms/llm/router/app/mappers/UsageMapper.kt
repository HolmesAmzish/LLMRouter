package cn.arorms.llm.router.app.mappers

import cn.arorms.llm.router.app.entities.UsageRecord
import cn.arorms.llm.router.common.responses.UsageResponse

/**
 * Converts usage persistence objects into API responses.
 */
object UsageMapper {
    fun toResponse(record: UsageRecord): UsageResponse = UsageResponse(
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
        inputTokens = record.inputTokens,
        outputTokens = record.outputTokens,
        totalTokens = record.totalTokens,
        costCents = record.costCents,
        latencyMs = record.latencyMs,
        firstTokenMs = record.firstTokenMs,
        apiBase = record.apiBase,
        cacheHit = record.cacheHit ?: false,
        status = record.status,
        statusCode = record.statusCode,
        createdAt = record.createdAt?.toString()
    )
}
