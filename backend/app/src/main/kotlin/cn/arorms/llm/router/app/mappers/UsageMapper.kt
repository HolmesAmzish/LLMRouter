package cn.arorms.llm.router.app.mappers

import cn.arorms.llm.router.app.entities.UsageRecord
import cn.arorms.llm.router.common.responses.UsageResponse

/**
 * Converts usage persistence objects into API responses.
 */
object UsageMapper {
    fun toResponse(record: UsageRecord): UsageResponse = UsageResponse(
        id = record.id ?: 0L,
        sessionId = record.sessionId,
        provider = record.provider,
        accountName = record.accountName,
        model = record.model,
        protocol = record.protocol,
        inputTokens = record.inputTokens,
        outputTokens = record.outputTokens,
        totalTokens = record.totalTokens,
        costCents = record.costCents,
        latencyMs = record.latencyMs,
        status = record.status,
        createdAt = record.createdAt?.toString()
    )
}
